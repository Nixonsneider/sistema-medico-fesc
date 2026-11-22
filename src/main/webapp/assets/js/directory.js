import { request } from './api.js';
import { setFeedback, setBusy, validateForm, showFieldErrors, confirmAction } from './ui.js';

const element = id => document.getElementById(id);
const definitions = {
    especialidades: { title: 'Especialidades', description: 'Administra el catálogo y la disponibilidad de las especialidades.', singular: 'especialidad', template: 'especialidad-form-template', columns: ['Nombre', 'Descripción', 'Estado', 'Acciones'] },
    medicos: { title: 'Médicos', description: 'Registra y consulta los profesionales y sus especialidades.', singular: 'médico', template: 'medico-form-template', columns: ['Médico', 'Documento', 'Registro médico', 'Especialidad', 'Acciones'] }
};
let kind, page = 1, total = 0, query = '', filter = '', specialties = [], generation = 0, controller;
const pageSize = 20;

function beginRequest() {
    controller?.abort();
    controller = new AbortController();
    return { generation: ++generation, signal: controller.signal };
}
function current(operation) { return operation.generation === generation; }
function option(select, value, label) {
    const item = document.createElement('option');
    item.value = value;
    item.textContent = label;
    select.append(item);
}
function showList() {
    element('directory-list').hidden = false;
    element('directory-editor').hidden = true;
    element('directory-new').hidden = false;
    element('editor-form').replaceChildren();
}
function message(error) { return error.message || 'No se pudo completar la operación. Inténtalo de nuevo.'; }

export function closeDirectory() {
    controller?.abort();
    generation++;
    kind = null;
}
export async function openDirectory(selected) {
    kind = selected;
    page = 1; query = ''; filter = '';
    const operation = beginRequest();
    const definition = definitions[kind];
    showList();
    setFeedback(element('directory-feedback'));
    element('directory-rows').replaceChildren();
    element('directory-summary').textContent = '';
    element('directory-empty').hidden = true;
    element('directory-loading').hidden = false;
    element('directory-list').setAttribute('aria-busy', 'true');
    element('directory-retry').hidden = true;
    element('directory-previous').disabled = true;
    element('directory-next').disabled = true;
    [...element('directory-filters').elements].forEach(field => { field.disabled = true; });
    element('directory-title').textContent = definition.title;
    element('directory-description').textContent = definition.description;
    element('directory-new').textContent = `Registrar ${definition.singular}`;
    element('directory-new').disabled = true;
    element('directory-search').value = '';
    element('directory-search').placeholder = kind === 'medicos' ? 'Nombre, documento o registro médico' : 'Nombre de la especialidad';
    element('directory-filter-label').textContent = kind === 'medicos' ? 'Especialidad' : 'Estado';
    element('directory-caption').textContent = definition.title;
    element('directory-columns').replaceChildren(...definition.columns.map(label => {
        const th = document.createElement('th'); th.scope = 'col'; th.textContent = label; return th;
    }));
    element('directory-filter').replaceChildren();
    option(element('directory-filter'), '', kind === 'medicos' ? 'Todas las especialidades' : 'Todos los estados');
    try {
        if (kind === 'medicos') {
            specialties = [];
            let nextPage = 1, result;
            do {
                result = await request(`especialidades?pageSize=100&page=${nextPage++}`, { signal: operation.signal });
                if (!current(operation)) return;
                specialties.push(...result.items);
            } while (specialties.length < result.total && result.items.length);
            specialties.forEach(item => option(element('directory-filter'), item.idEspecialidad, item.nombre + (item.activo ? '' : ' (inactiva)')));
        } else {
            option(element('directory-filter'), 'true', 'Activas');
            option(element('directory-filter'), 'false', 'Inactivas');
        }
        if (current(operation)) {
            [...element('directory-filters').elements].forEach(field => { field.disabled = false; });
            element('directory-new').disabled = false;
            await loadList();
        }
    } catch (error) {
        if (current(operation) && error.name !== 'AbortError') {
            element('directory-loading').hidden = true;
            element('directory-list').setAttribute('aria-busy', 'false');
            setFeedback(element('directory-feedback'), message(error), 'error');
            element('directory-retry').hidden = false;
        }
    }
}

async function loadList() {
    if (!kind) return;
    const operation = beginRequest();
    const params = new URLSearchParams({ q: query, page, pageSize });
    if (filter) params.set(kind === 'medicos' ? 'idEspecialidad' : 'activo', filter);
    element('directory-loading').hidden = false;
    element('directory-list').setAttribute('aria-busy', 'true');
    element('directory-empty').hidden = true;
    element('directory-rows').replaceChildren();
    element('directory-summary').textContent = '';
    element('directory-previous').disabled = true;
    element('directory-next').disabled = true;
    element('directory-retry').hidden = true;
    try {
        const result = await request(`${kind}?${params}`, { signal: operation.signal });
        if (!current(operation)) return;
        total = result.total;
        const lastPage = Math.max(1, Math.ceil(total / pageSize));
        if (page > lastPage) { page = lastPage; return loadList(); }
        renderRows(result.items);
        element('directory-empty').textContent = query || filter ? 'No hay registros que coincidan con los filtros.' : (kind === 'especialidades' ? 'Todavía no hay especialidades registradas.' : 'Todavía no hay médicos registrados.');
        element('directory-empty').hidden = result.items.length !== 0;
        element('directory-summary').textContent = total ? `${(page - 1) * pageSize + 1}–${Math.min(page * pageSize, total)} de ${total} ${total === 1 ? 'registro' : 'registros'} · Página ${page}` : '0 registros';
        element('directory-previous').disabled = page <= 1;
        element('directory-next').disabled = page >= lastPage;
    } catch (error) {
        if (current(operation) && error.name !== 'AbortError') {
            setFeedback(element('directory-feedback'), message(error), 'error');
            element('directory-retry').hidden = false;
        }
    } finally {
        if (current(operation)) {
            element('directory-loading').hidden = true;
            element('directory-list').setAttribute('aria-busy', 'false');
        }
    }
}

function renderRows(items) {
    const rows = items.map(item => {
        const row = document.createElement('tr');
        const id = kind === 'medicos' ? item.idMedico : item.idEspecialidad;
        const values = kind === 'medicos' ? [`${item.nombre} ${item.apellido}`, item.documento, item.registroMedico, item.especialidad.nombre + (item.especialidad.activo ? '' : ' (inactiva)')] : [item.nombre, item.descripcion || 'Sin descripción', item.activo ? 'Activa' : 'Inactiva'];
        values.forEach((value, index) => {
            const cell = document.createElement(index === 0 ? 'th' : 'td');
            if (index === 0) cell.scope = 'row';
            if (kind === 'especialidades' && index === 2) {
                const badge = document.createElement('span'); badge.className = `badge ${item.activo ? 'badge-success' : 'badge-neutral'}`; badge.textContent = value; cell.append(badge);
            } else { cell.textContent = value; }
            if (kind === 'especialidades' && index === 1) cell.className = 'directory-description-cell';
            row.append(cell);
        });
        const cell = document.createElement('td');
        const actions = document.createElement('div'); actions.className = 'row-actions';
        [['Ver', 'view'], ['Editar', 'edit']].forEach(([label, mode]) => {
            const button = document.createElement('button'); button.type = 'button'; button.className = 'button button-secondary button-small';
            button.textContent = label; button.setAttribute('aria-label', `${label} ${values[0]}`);
            button.addEventListener('click', () => openEditor(mode, id)); actions.append(button);
        });
        cell.append(actions); row.append(cell); return row;
    });
    element('directory-rows').replaceChildren(...rows);
}

async function openEditor(mode, id) {
    const operation = beginRequest();
    const selected = kind;
    setFeedback(element('directory-feedback'));
    element('directory-retry').hidden = true;
    try {
        const data = id ? await request(`${kind}/${id}`, { signal: operation.signal }) : null;
        if (!current(operation)) return;
        element('directory-list').hidden = true;
        element('directory-editor').hidden = false;
        element('directory-new').hidden = true;
        element('editor-title').textContent = `${mode === 'view' ? 'Consultar' : mode === 'edit' ? 'Editar' : 'Registrar'} ${definitions[kind].singular}`;
        element('editor-form').replaceChildren(element(definitions[kind].template).content.cloneNode(true));
        const form = element('editor-form').querySelector('form');
        setFeedback(element('editor-feedback'));
        if (kind === 'medicos') {
            const select = form.elements.namedItem('idEspecialidad');
            const currentSpecialty = data?.especialidad;
            const choices = specialties.filter(item => item.activo || item.idEspecialidad === currentSpecialty?.idEspecialidad);
            if (currentSpecialty && !choices.some(item => item.idEspecialidad === currentSpecialty.idEspecialidad)) choices.push(currentSpecialty);
            choices.forEach(item => option(select, item.idEspecialidad, item.nombre + (item.activo ? '' : ' (inactiva; asignación actual)')));
            if (!choices.length) setFeedback(element('editor-feedback'), 'Primero registra una especialidad activa desde el menú Especialidades.', 'info');
            form.querySelector('[data-save]').disabled = !choices.length;
        }
        for (const field of form.elements) {
            if (!field.name) continue;
            const value = field.name === 'idEspecialidad' ? data?.especialidad.idEspecialidad : data?.[field.name];
            if (value != null) field.value = String(value);
            if (mode === 'view') {
                if (field.matches('select')) field.disabled = true;
                else field.readOnly = true;
            }
        }
        form.querySelector('[data-save]').hidden = mode === 'view';
        form.querySelector('[data-cancel]').textContent = mode === 'view' ? 'Volver al listado' : 'Cancelar';
        form.querySelector('[data-cancel]').addEventListener('click', () => { beginRequest(); showList(); loadList(); element('directory-title').focus(); });
        form.addEventListener('submit', event => { event.preventDefault(); if (mode !== 'view') save(form, data, selected, id); });
        element('editor-title').focus();
    } catch (error) {
        if (current(operation) && error.name !== 'AbortError') setFeedback(element('directory-feedback'), message(error), 'error');
    }
}

async function save(form, original, selected, id) {
    if (form.dataset.saving || !validateForm(form)) return;
    const payload = Object.fromEntries(new FormData(form));
    if (selected === 'especialidades') payload.activo = payload.activo === 'true';
    else payload.idEspecialidad = Number(payload.idEspecialidad);
    form.dataset.saving = 'true';
    const saveButton = form.querySelector('[data-save]');
    const cancelButton = form.querySelector('[data-cancel]');
    const fieldsets = [...form.querySelectorAll('fieldset')];
    setBusy(saveButton, true);
    cancelButton.disabled = true;
    fieldsets.forEach(fieldset => { fieldset.disabled = true; });
    const operation = beginRequest();
    try {
        if (selected === 'especialidades' && original?.activo && !payload.activo) {
            const confirmed = await confirmAction({ title: 'Desactivar especialidad', message: 'Los médicos asociados conservarán su especialidad. No podrá asignarse a nuevos médicos ni en una reasignación.', confirmLabel: 'Desactivar' });
            if (!confirmed || !current(operation)) return;
        }
        await request(id ? `${selected}/${id}` : selected, { method: id ? 'PUT' : 'POST', body: payload, signal: operation.signal });
        if (!current(operation)) return;
        showList();
        setFeedback(element('directory-feedback'), `${selected === 'especialidades' ? 'Especialidad guardada' : 'Médico guardado'} correctamente.`, 'success');
        await loadList();
        element('directory-title').focus();
    } catch (error) {
        if (current(operation) && error.name !== 'AbortError') {
            fieldsets.forEach(fieldset => { fieldset.disabled = false; });
            setFeedback(element('editor-feedback'), message(error), 'error');
            showFieldErrors(form, error.fields);
        }
    } finally {
        delete form.dataset.saving;
        setBusy(saveButton, false);
        cancelButton.disabled = false;
        fieldsets.forEach(fieldset => { fieldset.disabled = false; });
    }
}

element('directory-new').addEventListener('click', () => openEditor('create'));
element('directory-retry').addEventListener('click', () => openDirectory(kind));
element('directory-filters').addEventListener('submit', event => {
    event.preventDefault(); query = element('directory-search').value.trim(); filter = element('directory-filter').value; page = 1;
    setFeedback(element('directory-feedback')); loadList();
});
element('directory-previous').addEventListener('click', () => { if (page > 1) { page--; loadList(); } });
element('directory-next').addEventListener('click', () => { if (page * pageSize < total) { page++; loadList(); } });
