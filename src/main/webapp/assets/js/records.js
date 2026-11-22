import { request } from './api.js';
import { setFeedback, setBusy, validateForm, showFieldErrors } from './ui.js';

const el = id => document.getElementById(id);
const config = {
    diagnosticos: { title: 'Diagnósticos', singular: 'diagnóstico', parent: 'citas', key: 'idCita', param: 'cita', template: 'diagnostico-form-template', columns: ['Código CIE-10', 'Paciente', 'Tipo', 'Cita', 'Acciones'] },
    tratamientos: { title: 'Tratamientos', singular: 'tratamiento', parent: 'diagnosticos', key: 'idDiagnostico', param: 'diagnostico', template: 'tratamiento-form-template', columns: ['Medicamento', 'Paciente', 'Dosis', 'Frecuencia', 'Días', 'Acciones'] }
};
const types = { PRESUNTIVO: 'Presuntivo', DEFINITIVO: 'Definitivo', PRINCIPAL: 'Principal', SECUNDARIO: 'Secundario' };
let kind, generation = 0, view = 0, sequence = 0, controller, page = 1, total = 0, references = [];
const size = 20;
const current = token => token === generation;
const read = path => request(path, { signal: controller.signal });
const message = error => error.message || 'No se pudo completar la operación.';
const option = (select, id, label) => { const item = document.createElement('option'); item.value = id; item.textContent = label; select.append(item); };
const date = value => new Intl.DateTimeFormat('es-CO', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Bogota' }).format(new Date(value));
function labelOf(data) { return kind === 'diagnosticos' ? `Cita #${data.idCita} · ${data.paciente.nombre} · ${date(data.fechaHora)}` : `Diagnóstico #${data.idDiagnostico} · ${data.codigoCie10} · ${data.paciente.nombre}`; }
function parentOf(data) { return kind === 'diagnosticos' ? data.idCita : data.diagnostico.idDiagnostico; }

export function closeRecords() { controller?.abort(); generation++; view++; sequence++; kind = null; }
export async function openRecords(selected, params) {
    closeRecords(); kind = selected; controller = new AbortController(); page = 1;
    const token = generation, definition = config[kind];
    showList(); setFeedback(el('records-feedback'));
    el('records-title').textContent = definition.title;
    el('records-description').textContent = kind === 'diagnosticos' ? 'Registra los diagnósticos vinculados a una cita y consulta sus tratamientos.' : 'Consulta y registra los tratamientos de cada diagnóstico.';
    el('records-new').textContent = `Registrar ${definition.singular}`; el('records-new').disabled = true;
    el('records-filters').reset(); el('records-query').placeholder = kind === 'diagnosticos' ? 'Código CIE-10 o nombre del paciente' : 'Medicamento o código CIE-10';
    el('records-parent-label').textContent = kind === 'diagnosticos' ? 'Cita' : 'Diagnóstico';
    el('records-caption').textContent = definition.title;
    el('records-columns').replaceChildren(...definition.columns.map(text => { const cell = document.createElement('th'); cell.scope = 'col'; cell.textContent = text; return cell; }));
    el('records-rows').replaceChildren(); el('records-summary').textContent = ''; el('records-empty').hidden = true;
    el('records-previous').disabled = true; el('records-next').disabled = true;
    el('records-retry').hidden = true; el('records-loading').hidden = false;
    try {
        const id = params.get(definition.param);
        if (id && !/^[1-9]\d*$/.test(id)) throw new Error('El identificador de la referencia no es válido.');
        const source = id ? await read(`${definition.parent}/${id}`) : null;
        if (!current(token)) return;
        await loadReferences(id, source ? labelOf(source) : null);
        if (!current(token)) return;
        el('records-new').disabled = false;
        await loadList();
    } catch (error) {
        if (current(token) && error.name !== 'AbortError') { el('records-loading').hidden = true; setFeedback(el('records-feedback'), message(error), 'error'); el('records-retry').hidden = false; }
    }
}

async function loadReferences(selected = el('records-parent-filter').value, knownLabel = null) {
    const token = generation;
    const select = el('records-parent-filter'), button = el('records-source-search');
    const oldLabel = knownLabel || select.selectedOptions[0]?.textContent;
    setBusy(button, true, 'Buscando…'); select.disabled = true;
    try {
        const items = await read(`${config[kind].parent}/opciones?q=${encodeURIComponent(el('records-source-query').value.trim())}`);
        if (!current(token)) return;
        references = items;
        select.replaceChildren(); option(select, '', 'Todos los registros');
        items.forEach(item => option(select, item.id, item.etiqueta));
        if (selected && !items.some(item => String(item.id) === String(selected))) option(select, selected, oldLabel);
        select.value = selected || '';
        el('records-source-hint').textContent = `${items.length} coincidencias (máximo 100). Refina la búsqueda si es necesario.`;
    } finally { if (current(token)) { setBusy(button, false); select.disabled = false; } }
}
function showList() { el('records-list').hidden = false; el('records-editor').hidden = true; el('records-new').hidden = false; el('records-form').replaceChildren(); }
async function loadList() {
    const token = generation, serial = ++sequence;
    const params = new URLSearchParams({ q: el('records-query').value.trim(), page, pageSize: size });
    if (el('records-parent-filter').value) params.set(config[kind].key, el('records-parent-filter').value);
    el('records-loading').hidden = false; el('records-list').setAttribute('aria-busy', 'true');
    el('records-rows').replaceChildren(); el('records-empty').hidden = true; el('records-retry').hidden = true;
    el('records-previous').disabled = true; el('records-next').disabled = true;
    try {
        const data = await read(`${kind}?${params}`);
        if (!current(token) || serial !== sequence) return;
        total = data.total;
        const last = Math.max(1, Math.ceil(total / size));
        if (page > last) { page = last; return loadList(); }
        data.items.forEach(item => {
            const row = document.createElement('tr');
            const id = kind === 'diagnosticos' ? item.idDiagnostico : item.idTratamiento;
            const values = kind === 'diagnosticos' ? [item.codigoCie10, item.paciente.nombre, types[item.tipo], `#${item.idCita} · ${date(item.fechaCita)}`] : [item.medicamento, item.diagnostico.paciente.nombre, item.dosis, item.frecuencia, String(item.duracionDias)];
            values.forEach((text, index) => { const cell = document.createElement(index === 0 ? 'th' : 'td'); if (index === 0) cell.scope = 'row'; cell.textContent = text; row.append(cell); });
            const cell = document.createElement('td'), actions = document.createElement('div'); actions.className = 'row-actions';
            [['Ver', 'view'], ['Editar', 'edit']].forEach(([text, mode]) => {
                const button = document.createElement('button'); button.type = 'button'; button.className = 'button button-secondary button-small'; button.textContent = text;
                button.setAttribute('aria-label', `${text} ${config[kind].singular} ${values[0]}`); button.addEventListener('click', () => editor(mode, id)); actions.append(button);
            });
            if (kind === 'diagnosticos') {
                const link = document.createElement('a'); link.href = `#tratamientos?diagnostico=${id}`; link.className = 'button button-secondary button-small'; link.textContent = 'Tratamientos'; link.setAttribute('aria-label', `Tratamientos del diagnóstico ${item.codigoCie10}`); actions.append(link);
            }
            cell.append(actions); row.append(cell); el('records-rows').append(row);
        });
        el('records-empty').hidden = data.items.length > 0; el('records-empty').textContent = 'No hay registros para esta selección. Puedes registrar uno nuevo o cambiar los filtros.';
        el('records-summary').textContent = total ? `${(page-1)*size+1}–${Math.min(page*size,total)} de ${total} registros · Página ${page}` : '0 registros';
        el('records-previous').disabled = page <= 1; el('records-next').disabled = page >= last;
    } catch (error) { if (current(token) && serial === sequence && error.name !== 'AbortError') { setFeedback(el('records-feedback'), message(error), 'error'); el('records-retry').hidden = false; } }
    finally { if (current(token) && serial === sequence) { el('records-loading').hidden = true; el('records-list').setAttribute('aria-busy', 'false'); } }
}

async function editor(mode, id) {
    const token = generation, serial = ++view, definition = config[kind];
    try {
        const data = id ? await read(`${kind}/${id}`) : null;
        if (!current(token) || serial !== view) return;
        sequence++; el('records-list').hidden = true; el('records-editor').hidden = false; el('records-new').hidden = true;
        setFeedback(el('records-feedback')); setFeedback(el('records-editor-feedback'));
        el('records-editor-title').textContent = `${mode === 'view' ? 'Consultar' : mode === 'edit' ? 'Editar' : 'Registrar'} ${definition.singular}`;
        el('records-form').replaceChildren(el(definition.template).content.cloneNode(true));
        const form = el('records-form').querySelector('form'), parent = form.querySelector('[data-parent]');
        parent.name = definition.key;
        const source = data ? parentOf(data) : Number(el('records-parent-filter').value) || null;
        references.forEach(item => option(parent, item.id, item.etiqueta));
        if (source && !references.some(item => Number(item.id) === Number(source))) {
            const text = data ? (kind === 'diagnosticos' ? `Cita #${data.idCita} · ${data.paciente.nombre}` : `Diagnóstico #${source} · ${data.diagnostico.codigoCie10}`) : el('records-parent-filter').selectedOptions[0].textContent;
            option(parent, source, text);
        }
        if (source) parent.value = String(source);
        parent.disabled = Boolean(id || source);
        for (const field of form.elements) {
            if (field.name && data?.[field.name] != null) field.value = String(data[field.name]);
            if (mode === 'view') { if (field.matches('select')) field.disabled = true; else if (field.matches('input,textarea')) field.readOnly = true; }
        }
        form.querySelector('[data-save]').hidden = mode === 'view';
        if (!source && !references.length) { form.querySelector('[data-save]').disabled = true; setFeedback(el('records-editor-feedback'), 'Primero selecciona una referencia existente mediante la búsqueda del listado.', 'info'); }
        form.querySelector('[data-cancel]').textContent = mode === 'view' ? 'Volver al listado' : 'Cancelar';
        form.querySelector('[data-cancel]').addEventListener('click', () => { view++; showList(); loadList(); el('records-title').focus(); });
        form.addEventListener('submit', event => { event.preventDefault(); if (mode !== 'view') save(form, id, source, token, serial); });
        el('records-editor-title').focus();
    } catch (error) { if (current(token) && error.name !== 'AbortError') setFeedback(el('records-feedback'), message(error), 'error'); }
}
async function save(form, id, source, token, serial) {
    if (form.dataset.saving || !validateForm(form)) return;
    const selected = kind, definition = config[selected], payload = Object.fromEntries(new FormData(form));
    payload[definition.key] = source || Number(payload[definition.key]);
    if (selected === 'tratamientos') payload.duracionDias = Number(payload.duracionDias);
    form.dataset.saving = 'true'; const button = form.querySelector('[data-save]'), cancel = form.querySelector('[data-cancel]'), fieldset = form.querySelector('fieldset');
    setBusy(button, true); cancel.disabled = true; fieldset.disabled = true;
    try {
        await request(id ? `${selected}/${id}` : selected, { method: id ? 'PUT' : 'POST', body: payload });
        if (!current(token) || serial !== view) return;
        view++; showList(); setFeedback(el('records-feedback'), selected === 'diagnosticos' ? 'Diagnóstico guardado correctamente.' : 'Tratamiento guardado correctamente.', 'success');
        await loadList(); el('records-title').focus();
    } catch (error) { if (current(token) && serial === view) { fieldset.disabled = false; setFeedback(el('records-editor-feedback'), message(error), 'error'); showFieldErrors(form, error.fields); } }
    finally { delete form.dataset.saving; setBusy(button, false); cancel.disabled = false; fieldset.disabled = false; }
}
el('records-new').addEventListener('click', () => editor('create'));
el('records-filters').addEventListener('submit', event => { event.preventDefault(); page = 1; setFeedback(el('records-feedback')); loadList(); });
el('records-parent-filter').addEventListener('change', () => { page = 1; loadList(); });
async function searchReferences() { try { await loadReferences(); } catch (error) { if (error.name !== 'AbortError') setFeedback(el('records-feedback'), message(error), 'error'); } }
el('records-source-search').addEventListener('click', searchReferences);
el('records-source-query').addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); searchReferences(); } });
el('records-retry').addEventListener('click', () => openRecords(kind,new URLSearchParams(location.hash.split('?')[1] || '')));
el('records-previous').addEventListener('click', () => { if (page>1) { page--; loadList(); } });
el('records-next').addEventListener('click', () => { if (page*size<total) { page++; loadList(); } });
