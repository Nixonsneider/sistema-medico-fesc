import { request } from './api.js';
import { setFeedback, setBusy, validateForm, showFieldErrors, clearFieldErrors, confirmAction } from './ui.js';

const el = id => document.getElementById(id);
const names = { pacientes: 'Pacientes', historias: 'Historias clínicas', citas: 'Citas' };
const labels = { PROGRAMADA: 'Programada', CONFIRMADA: 'Confirmada', ATENDIDA: 'Atendida', CANCELADA: 'Cancelada', NO_ASISTIO: 'No asistió' };
let kind, route = 0, view = 0, listSequence = 0, controller, page = 1, total = 0, patientContext = null, currentAppointment;
const size = 20;
const valid = token => token === route;
const active = state => ['PROGRAMADA', 'CONFIRMADA'].includes(state);
const feedback = error => error.message || 'No se pudo completar la operación. Inténtalo de nuevo.';
const option = (select, value, text) => { const item = document.createElement('option'); item.value = value; item.textContent = text; select.append(item); };
const read = (path, options = {}) => request(path, { ...options, signal: controller.signal });
const patientOption = p => ({ id: p.idPaciente, nombre: `${p.nombre} ${p.apellido}`, documento: p.documento });

/** Referencias limitadas a 100 coincidencias: no carga fichas ni datos clínicos. */
async function lookup(container, endpoint, selected, token, editable = true, viewToken = null) {
    const select = container.querySelector('select');
    const search = container.querySelector('input[type="search"]');
    const button = container.querySelector('[data-lookup-search]');
    const hint = container.querySelector('.field-hint');
    let sequence = 0;
    const stillCurrent = () => valid(token) && (viewToken === null || viewToken === view);
    async function load() {
        const serial = ++sequence;
        const oldValue = select.value;
        const oldText = select.selectedOptions[0]?.textContent;
        select.disabled = true;
        setBusy(button, true, 'Buscando…');
        hint.textContent = 'Buscando coincidencias…';
        try {
            const result = await read(`${endpoint}/opciones?q=${encodeURIComponent(search.value.trim())}`);
            if (!stillCurrent() || serial !== sequence) return;
            select.replaceChildren(); option(select, '', 'Selecciona un registro');
            result.forEach(item => option(select, item.id, `${item.nombre} · ${item.documento}`));
            const keep = selected?.id || oldValue;
            if (keep && !result.some(item => String(item.id) === String(keep))) option(select, keep, selected ? `${selected.nombre} · ${selected.documento}` : oldText);
            if (keep) select.value = String(keep);
            selected = null;
            hint.textContent = result.length ? `${result.length} ${result.length === 1 ? 'coincidencia' : 'coincidencias'} (máximo 100). Refina la búsqueda si es necesario.` : 'No hay coincidencias. Busca otro nombre o documento.';
        } catch (error) {
            if (stillCurrent() && error.name !== 'AbortError') hint.textContent = feedback(error);
        } finally {
            if (stillCurrent() && serial === sequence) { select.disabled = !editable; setBusy(button, false); }
        }
    }
    button.addEventListener('click', load);
    search.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); load(); } });
    search.disabled = !editable; button.disabled = !editable;
    await load();
    if (!editable) { search.disabled = true; button.disabled = true; }
}

export function closeClinical() { controller?.abort(); route++; view++; listSequence++; kind = null; }

export async function openClinical(selected, params) {
    closeClinical();
    kind = selected; controller = new AbortController();
    const token = route;
    page = 1; patientContext = null;
    el('clinical-title').textContent = names[kind];
    el('clinical-category').textContent = kind === 'historias' ? 'Atención clínica' : 'Administración';
    el('clinical-description').textContent = { pacientes: 'Identificación, contacto e información relacionada de cada paciente.', historias: 'Consulta y actualiza la información clínica del paciente seleccionado.', citas: 'Programa la atención y consulta la agenda del consultorio. Horario de Bogotá.' }[kind];
    setFeedback(el('clinical-feedback')); el('clinical-retry').hidden = true;
    el('clinical-editor').hidden = true; el('clinical-related').hidden = true; el('clinical-context').hidden = true;
    el('history-picker').hidden = kind !== 'historias';
    el('clinical-list').hidden = kind === 'historias'; el('clinical-new').hidden = kind === 'historias';
    el('clinical-new').textContent = kind === 'pacientes' ? 'Registrar paciente' : 'Programar cita';
    el('clinical-new').disabled = true;
    el('clinical-filters').reset(); el('appointment-filters').hidden = kind !== 'citas';
    el('clinical-rows').replaceChildren(); el('clinical-summary').textContent = '';
    el('clinical-empty').hidden = true; el('clinical-loading').hidden = kind === 'historias';
    el('clinical-previous').disabled = true; el('clinical-next').disabled = true;
    const columns = kind === 'pacientes' ? ['Paciente', 'Documento', 'Nacimiento', 'Acciones'] : ['Fecha y hora (Bogotá)', 'Paciente', 'Médico', 'Estado', 'Acciones'];
    el('clinical-columns').replaceChildren(...columns.map(text => { const th = document.createElement('th'); th.scope = 'col'; th.textContent = text; return th; }));
    el('clinical-caption').textContent = names[kind];
    try {
        const patient = params.get('paciente');
        if (patient && !/^[1-9]\d*$/.test(patient)) throw new Error('El identificador del paciente no es válido.');
        if (patient) patientContext = patientOption(await read(`pacientes/${patient}`));
        if (!valid(token)) return;
        if (kind === 'historias') {
            el('history-patient-value').value = '';
            await lookup(el('history-patient'), 'pacientes', patientContext, token);
            if (valid(token) && patientContext) await history(patientContext.id);
        } else {
            if (kind === 'citas') {
                await lookup(el('filter-doctor'), 'medicos', null, token);
                if (!valid(token)) return;
                if (patientContext) {
                    el('clinical-context').hidden = false;
                    el('clinical-context-text').textContent = `Citas de ${patientContext.nombre} · ${patientContext.documento}`;
                }
            }
            el('clinical-new').disabled = false;
            await loadList();
            if (valid(token) && kind === 'pacientes' && patientContext) await editor('view', patientContext.id);
        }
    } catch (error) {
        if (valid(token) && error.name !== 'AbortError') { el('clinical-loading').hidden = true; setFeedback(el('clinical-feedback'), feedback(error), 'error'); el('clinical-retry').hidden = false; }
    }
}

function listParams() {
    const params = new URLSearchParams({ q: el('clinical-query').value.trim(), page, pageSize: size });
    if (kind === 'citas') {
        if (patientContext) params.set('idPaciente', patientContext.id);
        for (const [key, id] of [['desde', 'clinical-from'], ['hasta', 'clinical-to'], ['estado', 'clinical-state'], ['idMedico', 'filter-doctor-value']]) if (el(id).value) params.set(key, el(id).value);
    }
    return params;
}
async function loadList() {
    const token = route, serial = ++listSequence;
    el('clinical-loading').hidden = false; el('clinical-list').setAttribute('aria-busy', 'true');
    el('clinical-empty').hidden = true; el('clinical-rows').replaceChildren();
    el('clinical-previous').disabled = true; el('clinical-next').disabled = true;
    el('clinical-retry').hidden = true;
    try {
        const data = await read(`${kind}?${listParams()}`);
        if (!valid(token) || serial !== listSequence) return;
        total = data.total;
        const last = Math.max(1, Math.ceil(total / size));
        if (page > last) { page = last; return loadList(); }
        data.items.forEach(item => {
            const row = document.createElement('tr');
            const values = kind === 'pacientes' ? [`${item.nombre} ${item.apellido}`, `${item.tipoDocumento} ${item.documento}`, item.fechaNacimiento] : [formatDate(item.fechaHora), item.paciente.nombre, item.medico.nombre, labels[item.estadoCita]];
            values.forEach((text, index) => {
                const cell = document.createElement(index === 0 ? 'th' : 'td'); if (index === 0) cell.scope = 'row';
                if (kind === 'citas' && index === 3) { const badge = document.createElement('span'); badge.className = `badge ${item.estadoCita === 'ATENDIDA' ? 'badge-success' : item.estadoCita === 'CANCELADA' ? 'badge-danger' : 'badge-neutral'}`; badge.textContent = text; cell.append(badge); }
                else cell.textContent = text;
                row.append(cell);
            });
            const cell = document.createElement('td'), actions = document.createElement('div'); actions.className = 'row-actions';
            const id = kind === 'pacientes' ? item.idPaciente : item.idCita;
            [['Ver', 'view'], [kind === 'citas' ? 'Reprogramar' : 'Editar', 'edit']].forEach(([text, mode]) => {
                if (mode === 'edit' && kind === 'citas' && !active(item.estadoCita)) return;
                const button = document.createElement('button'); button.type = 'button'; button.className = 'button button-secondary button-small'; button.textContent = text;
                button.setAttribute('aria-label', `${text} ${kind === 'pacientes' ? values[0] : 'cita de ' + item.paciente.nombre}`);
                button.addEventListener('click', () => editor(mode, id)); actions.append(button);
            });
            cell.append(actions); row.append(cell); el('clinical-rows').append(row);
        });
        el('clinical-empty').textContent = 'No hay registros para esta búsqueda. Puedes registrar uno nuevo o cambiar los filtros.';
        el('clinical-empty').hidden = data.items.length !== 0;
        el('clinical-summary').textContent = total ? `${(page - 1) * size + 1}–${Math.min(page * size, total)} de ${total} ${total === 1 ? 'registro' : 'registros'} · Página ${page}` : '0 registros';
        el('clinical-previous').disabled = page <= 1; el('clinical-next').disabled = page >= last;
    } catch (error) {
        if (valid(token) && serial === listSequence && error.name !== 'AbortError') { setFeedback(el('clinical-feedback'), feedback(error), 'error'); showFieldErrors(el('clinical-filters'), error.fields); el('clinical-retry').hidden = false; }
    } finally {
        if (valid(token) && serial === listSequence) { el('clinical-loading').hidden = true; el('clinical-list').setAttribute('aria-busy', 'false'); }
    }
}

function formatDate(value) { return new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Bogota' }).format(new Date(value)); }
function localDateTime(value) {
    const parts = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Bogota', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).formatToParts(new Date(value));
    const part = type => parts.find(item => item.type === type).value;
    return `${part('year')}-${part('month')}-${part('day')}T${part('hour')}:${part('minute')}`;
}
function showEditor(template, title) {
    listSequence++;
    el('clinical-list').hidden = true; el('clinical-editor').hidden = false; el('clinical-new').hidden = true;
    el('clinical-related').hidden = true; el('appointment-state-panel').hidden = true;
    setFeedback(el('clinical-editor-feedback')); setFeedback(el('clinical-feedback'));
    el('clinical-editor-title').textContent = title;
    el('clinical-form').replaceChildren(el(template).content.cloneNode(true));
    return el('clinical-form').querySelector('form');
}
function fill(form, data, readonly) {
    for (const field of form.elements) {
        if (field.name && data?.[field.name] != null) field.value = String(data[field.name]);
        if (readonly) { if (field.matches('select')) field.disabled = true; else if (field.matches('input,textarea')) field.readOnly = true; }
    }
    form.querySelector('[data-save]').hidden = readonly;
    form.querySelector('[data-cancel]').textContent = readonly ? 'Volver al listado' : 'Cancelar';
}
function related(patient) {
    const links = el('clinical-related'); links.replaceChildren(); links.hidden = false;
    for (const [label, routeName] of [['Historia clínica', 'historias'], ['Citas del paciente', 'citas']]) {
        const anchor = document.createElement('a'); anchor.className = 'button button-secondary'; anchor.textContent = label; anchor.href = `#${routeName}?paciente=${patient}`; links.append(anchor);
    }
}
async function editor(mode, id) {
    const token = route, serial = ++view;
    try {
        const data = id ? await read(`${kind}/${id}`) : null;
        if (!valid(token) || serial !== view) return;
        const isPatient = kind === 'pacientes';
        const form = showEditor(isPatient ? 'paciente-form-template' : 'cita-form-template', `${mode === 'view' ? 'Consultar' : mode === 'edit' ? 'Editar' : 'Registrar'} ${isPatient ? 'paciente' : 'cita'}`);
        fill(form, isPatient ? data : data ? { ...data, fechaHora: localDateTime(data.fechaHora) } : null, mode === 'view');
        const saveButton = form.querySelector('[data-save]'); saveButton.disabled = true;
        if (isPatient) {
            form.elements.fechaNacimiento.max = localDateTime(new Date()).slice(0, 10);
            if (id) related(id);
        } else {
            await Promise.all([
                lookup(el('a-patient'), 'pacientes', data?.paciente || patientContext, token, mode !== 'view', serial),
                lookup(el('a-doctor'), 'medicos', data?.medico, token, mode !== 'view', serial)
            ]);
            if (!valid(token) || serial !== view) return;
            if (mode !== 'view') form.elements.fechaHora.min = localDateTime(new Date(Date.now() + 60000));
            if (mode === 'view') { currentAppointment = data; showState(data); }
        }
        saveButton.disabled = false;
        form.querySelector('[data-cancel]').addEventListener('click', backToList);
        form.addEventListener('submit', event => { event.preventDefault(); if (mode !== 'view') saveForm(form, kind, id, token); });
        el('clinical-editor-title').focus();
    } catch (error) { if (valid(token) && error.name !== 'AbortError') setFeedback(el('clinical-feedback'), feedback(error), 'error'); }
}

function backToList() {
    view++; listSequence++; el('clinical-editor').hidden = true; el('clinical-list').hidden = false; el('clinical-new').hidden = false;
    el('clinical-form').replaceChildren(); loadList(); el('clinical-title').focus();
}
async function saveForm(form, selected, id, token, path = null) {
    if (form.dataset.saving || !validateForm(form)) return;
    const payload = Object.fromEntries(new FormData(form));
    if (selected === 'citas') { payload.idPaciente = Number(payload.idPaciente); payload.idMedico = Number(payload.idMedico); payload.fechaHora += ':00-05:00'; }
    form.dataset.saving = 'true';
    const button = form.querySelector('[data-save]'), cancel = form.querySelector('[data-cancel]'), fieldsets = [...form.querySelectorAll('fieldset')];
    setBusy(button, true); cancel.disabled = true; fieldsets.forEach(item => { item.disabled = true; });
    const serial = view;
    try {
        await request(path || (id ? `${selected}/${id}` : selected), { method: id ? 'PUT' : 'POST', body: payload });
        if (!valid(token) || serial !== view) return;
        if (selected === 'historias') { await history(Number(el('history-patient-value').value)); setFeedback(el('clinical-feedback'), 'Historia clínica guardada correctamente.', 'success'); }
        else { backToList(); setFeedback(el('clinical-feedback'), selected === 'pacientes' ? 'Paciente guardado correctamente.' : 'Cita guardada correctamente.', 'success'); }
    } catch (error) {
        if (valid(token) && serial === view) { fieldsets.forEach(item => { item.disabled = false; }); setFeedback(el('clinical-editor-feedback'), feedback(error), 'error'); showFieldErrors(form, error.fields); }
    } finally { delete form.dataset.saving; setBusy(button, false); cancel.disabled = false; fieldsets.forEach(item => { item.disabled = false; }); }
}

async function history(patient, edit = false) {
    const token = route, serial = ++view;
    el('clinical-editor').hidden = true;
    if (!patient) return;
    try {
        const [owner, data] = await Promise.all([read(`pacientes/${patient}`), read(`pacientes/${patient}/historia`)]);
        if (!valid(token) || serial !== view) return;
        const form = showEditor('historia-form-template', `${data ? (edit ? 'Editar' : 'Consultar') : 'Abrir'} historia de ${owner.nombre} ${owner.apellido}`);
        fill(form, data, Boolean(data) && !edit);
        el('history-opening').textContent = data ? `Fecha de apertura: ${data.fechaApertura}` : 'La fecha de apertura se registra automáticamente al guardar.';
        const cancel = form.querySelector('[data-cancel]'); cancel.textContent = 'Volver al paciente'; cancel.addEventListener('click', () => { location.hash = `pacientes?paciente=${patient}`; });
        if (data && !edit) {
            const button = document.createElement('button'); button.type = 'button'; button.className = 'button button-primary'; button.textContent = 'Editar historia clínica';
            button.addEventListener('click', () => history(patient, true)); form.querySelector('.form-actions').prepend(button);
        }
        form.addEventListener('submit', event => { event.preventDefault(); if (!data || edit) saveForm(form, 'historias', data?.idHistoria, token, `pacientes/${patient}/historia`); });
        el('clinical-editor-title').focus();
    } catch (error) { if (valid(token) && error.name !== 'AbortError') setFeedback(el('clinical-feedback'), feedback(error), 'error'); }
}
function showState(data) {
    el('appointment-state-panel').hidden = false; el('appointment-current-state').textContent = `Estado actual: ${labels[data.estadoCita]}`;
    const select = el('appointment-new-state'); select.replaceChildren();
    const choices = active(data.estadoCita) ? ['CONFIRMADA', 'ATENDIDA', 'CANCELADA', 'NO_ASISTIO'].filter(value => value !== data.estadoCita) : [];
    option(select, '', 'Selecciona un estado'); choices.forEach(value => option(select, value, labels[value]));
    el('appointment-state-form').hidden = !choices.length;
}
el('appointment-state-form').addEventListener('submit', async event => {
    event.preventDefault(); const form = event.currentTarget;
    if (form.dataset.saving || !validateForm(form)) return;
    const token = route, serial = view, state = el('appointment-new-state').value;
    form.dataset.saving = 'true'; const button = form.querySelector('button'); setBusy(button, true, 'Actualizando…');
    try {
        if (state === 'CANCELADA' && !await confirmAction({ title: 'Cancelar cita', message: 'La cita quedará cancelada. Para una nueva atención tendrás que programar otra cita.', confirmLabel: 'Cancelar cita' })) return;
        if (!valid(token) || serial !== view) return;
        const data = await request(`citas/${currentAppointment.idCita}/estado`, { method: 'PATCH', body: { estado: state } });
        if (!valid(token) || serial !== view) return;
        currentAppointment = data; showState(data); setFeedback(el('clinical-editor-feedback'), 'Estado actualizado correctamente.', 'success');
    } catch (error) { if (valid(token) && serial === view) { setFeedback(el('clinical-editor-feedback'), feedback(error), 'error'); showFieldErrors(form, error.fields); } }
    finally { delete form.dataset.saving; setBusy(button, false); }
});
el('history-patient-value').addEventListener('change', event => history(Number(event.target.value)));
el('clinical-new').addEventListener('click', () => editor('create'));
el('clinical-filters').addEventListener('submit', event => { event.preventDefault(); page = 1; clearFieldErrors(event.currentTarget); setFeedback(el('clinical-feedback')); loadList(); });
el('clinical-previous').addEventListener('click', () => { if (page > 1) { page--; loadList(); } });
el('clinical-next').addEventListener('click', () => { if (page * size < total) { page++; loadList(); } });
el('clinical-retry').addEventListener('click', () => openClinical(kind, new URLSearchParams(location.hash.split('?')[1] || '')));
