import { request } from './api.js';
import { setFeedback, setBusy, validateForm, showFieldErrors, confirmAction } from './ui.js';

const el = id => document.getElementById(id);
const types = { LABORATORIO: 'Laboratorio', IMAGENOLOGIA: 'Imagenología', MEDICAMENTO: 'Medicamento', REMISION: 'Remisión' };
const states = { PENDIENTE: 'Pendiente', COMPLETADA: 'Completada', CANCELADA: 'Cancelada' };
let generation = 0, sequence = 0, view = 0, referenceSequence = 0, controller, page = 1, total = 0, references = [];
const valid = token => token === generation;
const read = path => request(path, { signal: controller.signal });
const option = (select, value, label) => { const item = document.createElement('option'); item.value = value; item.textContent = label; select.append(item); };
function button(label, action, primary = false) {
    const item = document.createElement('button'); item.type = 'button'; item.className = `button button-${primary ? 'primary' : 'secondary'}`; item.textContent = label; item.addEventListener('click', action); return item;
}
function field(form, name, label, tag = 'input', required = true) {
    const wrapper = document.createElement('div'); wrapper.className = 'field';
    const input = document.createElement(tag); input.id = `order-${name}`; input.name = name; input.required = required;
    const caption = document.createElement('label'); caption.htmlFor = input.id; caption.textContent = `${label} (${required ? 'obligatorio' : 'opcional'})`;
    if (tag === 'input' || tag === 'textarea') input.maxLength = 10000;
    wrapper.append(caption, input); form.append(wrapper); return input;
}
export function closeOrders() { controller?.abort(); generation++; sequence++; view++; referenceSequence++; }
export async function openOrders(params) {
    closeOrders(); controller = new AbortController(); page = 1; references = [];
    const token = generation;
    listView(); el('orders-filters').reset(); el('orders-rows').replaceChildren(); el('orders-summary').textContent = '';
    el('orders-parent').replaceChildren(); option(el('orders-parent'), '', 'Todas las citas');
    setFeedback(el('orders-feedback')); el('orders-new').disabled = true;
    el('orders-loading').hidden = false; el('orders-empty').hidden = true; el('orders-retry').hidden = true;
    el('orders-previous').disabled = true; el('orders-next').disabled = true;
    try {
        const id = params.get('cita');
        if (id && !/^[1-9]\d*$/.test(id)) throw new Error('El identificador de cita no es válido.');
        const cita = id ? await read(`citas/${id}`) : null;
        if (!valid(token)) return;
        await loadReferences(id, cita ? `Cita #${id} · ${cita.paciente.nombre}` : null);
        if (!valid(token)) return;
        el('orders-new').disabled = false; await loadList();
    } catch (error) {
        if (valid(token) && error.name !== 'AbortError') { setFeedback(el('orders-feedback'), error.message, 'error'); el('orders-retry').hidden = false; el('orders-loading').hidden = true; }
    }
}
async function loadReferences(selected = el('orders-parent').value, label = null) {
    const token = generation, serial = ++referenceSequence, select = el('orders-parent'), search = el('orders-source-search');
    const known = label || select.selectedOptions[0]?.textContent;
    setBusy(search, true, 'Buscando…'); select.disabled = true;
    try {
        const items = await read(`citas/opciones?q=${encodeURIComponent(el('orders-source-query').value.trim())}`);
        if (!valid(token) || serial !== referenceSequence) return;
        references = items; select.replaceChildren(); option(select, '', 'Todas las citas');
        items.forEach(item => option(select, item.id, item.etiqueta));
        if (selected && !items.some(item => String(item.id) === String(selected))) option(select, selected, known);
        select.value = selected || '';
    } finally { if (valid(token) && serial === referenceSequence) { select.disabled = false; setBusy(search, false); } }
}
function listView() { view++; el('orders-list').hidden = false; el('orders-editor').hidden = true; el('orders-new').hidden = false; el('orders-form').replaceChildren(); }
async function loadList() {
    const token = generation, serial = ++sequence;
    const params = new URLSearchParams({ q: el('orders-query').value.trim(), page, pageSize: 20 });
    if (el('orders-parent').value) params.set('idCita', el('orders-parent').value);
    if (el('orders-state-filter').value) params.set('estado', el('orders-state-filter').value);
    el('orders-list').setAttribute('aria-busy', 'true'); el('orders-loading').hidden = false; el('orders-retry').hidden = true;
    el('orders-rows').replaceChildren(); el('orders-empty').hidden = true;
    el('orders-previous').disabled = true; el('orders-next').disabled = true;
    try {
        const data = await read(`ordenes?${params}`);
        if (!valid(token) || serial !== sequence) return;
        total = data.total;
        const last = Math.max(1, Math.ceil(total / 20));
        if (page > last) { page = last; return loadList(); }
        data.items.forEach(item => {
            const row = document.createElement('tr');
            [`#${item.idOrden}`, item.paciente.nombre, types[item.tipoOrden], item.fechaOrden, states[item.estado]].forEach((text, index) => {
                const cell = document.createElement(index ? 'td' : 'th'); if (!index) cell.scope = 'row'; cell.textContent = text; row.append(cell);
            });
            const cell = document.createElement('td'), action = button('Consultar', () => showOrder(item.idOrden)); action.setAttribute('aria-label', `Consultar orden ${item.idOrden}`); cell.append(action); row.append(cell); el('orders-rows').append(row);
        });
        el('orders-empty').hidden = total > 0;
        el('orders-summary').textContent = total ? `${(page - 1) * 20 + 1}–${Math.min(page * 20, total)} de ${total} órdenes · Página ${page}` : '0 órdenes';
        el('orders-previous').disabled = page <= 1; el('orders-next').disabled = page >= last;
    } catch (error) {
        if (valid(token) && serial === sequence && error.name !== 'AbortError') { setFeedback(el('orders-feedback'), error.message, 'error'); el('orders-retry').hidden = false; }
    } finally { if (valid(token) && serial === sequence) { el('orders-loading').hidden = true; el('orders-list').setAttribute('aria-busy', 'false'); } }
}
function editor(title) {
    view++; sequence++; el('orders-list').hidden = true; el('orders-editor').hidden = false; el('orders-new').hidden = true;
    el('orders-editor-title').textContent = title; el('orders-form').replaceChildren(); setFeedback(el('orders-editor-feedback')); setFeedback(el('orders-feedback'));
    el('orders-editor-title').focus(); return el('orders-form');
}
function back() { listView(); loadList(); el('orders-title').focus(); }
function createOrder() {
    const container = editor('Registrar orden'), token = generation, serial = view;
    const form = document.createElement('form'); form.noValidate = true;
    const fieldset = document.createElement('fieldset'), legend = document.createElement('legend'); legend.textContent = 'Datos de la orden'; fieldset.append(legend);
    const grid = document.createElement('div'); grid.className = 'form-grid'; fieldset.append(grid);
    const parent = field(grid, 'idCita', 'Cita', 'select'); option(parent, '', 'Selecciona una cita'); references.forEach(item => option(parent, item.id, item.etiqueta));
    const selected = el('orders-parent').value;
    if (selected && !references.some(item => String(item.id) === selected)) option(parent, selected, el('orders-parent').selectedOptions[0].textContent);
    if (selected) { parent.value = selected; parent.disabled = true; }
    const type = field(grid, 'tipoOrden', 'Tipo de orden', 'select'); option(type, '', 'Selecciona un tipo'); Object.entries(types).forEach(([value, label]) => option(type, value, label));
    const notes = field(grid, 'observaciones', 'Observaciones', 'textarea'); notes.parentElement.classList.add('field-full');
    const heading = document.createElement('h3'); heading.textContent = 'Detalles de la orden';
    const hint = document.createElement('p'); hint.className = 'field-hint'; hint.textContent = 'Añade al menos un detalle. Las cantidades deben ser enteros mayores que cero.';
    const details = document.createElement('div'); details.className = 'order-details';
    function renumber() {
        [...details.children].forEach((row, index) => {
            row.querySelector('legend').textContent = `Detalle ${index + 1}`;
            row.querySelectorAll('[data-detail-field]').forEach(input => {
                input.name = `detalles[${index}].${input.dataset.detailField}`; input.id = `order-detail-${index}-${input.dataset.detailField}`; input.previousElementSibling.htmlFor = input.id;
            });
            const remove = row.querySelector('button'); remove.disabled = details.children.length === 1; remove.setAttribute('aria-label', `Quitar detalle ${index + 1}`);
        });
    }
    function addDetail() {
        const row = document.createElement('fieldset'), title = document.createElement('legend'); row.className = 'order-detail'; row.append(title);
        const fields = document.createElement('div'); fields.className = 'form-grid'; row.append(fields);
        for (const [name, label, tag, required] of [['descripcion', 'Descripción', 'textarea', true], ['cantidad', 'Cantidad', 'input', true], ['indicaciones', 'Indicaciones', 'textarea', false]]) {
            const input = field(fields, name, label, tag, required); input.dataset.detailField = name;
            if (name === 'cantidad') { input.type = 'number'; input.min = '1'; input.step = '1'; input.max = '2147483647'; input.value = '1'; }
        }
        row.append(button('Quitar detalle', () => { row.remove(); renumber(); add.focus(); })); details.append(row); renumber();
    }
    const add = button('Añadir detalle', () => { addDetail(); details.lastElementChild.querySelector('textarea').focus(); });
    fieldset.append(heading, hint, details, add); form.append(fieldset);
    const actions = document.createElement('div'); actions.className = 'form-actions';
    const save = document.createElement('button'); save.type = 'submit'; save.className = 'button button-primary'; save.textContent = 'Guardar orden';
    const cancel = button('Cancelar', back); actions.append(save, cancel); form.append(actions); container.append(form); addDetail();
    if (!references.length && !selected) { save.disabled = true; setFeedback(el('orders-editor-feedback'), 'Primero busca y selecciona una cita desde el listado.', 'info'); }
    form.addEventListener('submit', async event => {
        event.preventDefault(); if (form.dataset.saving || !validateForm(form)) return;
        const payload = { idCita: Number(parent.value), tipoOrden: type.value, observaciones: notes.value, detalles: [...details.children].map(row => Object.fromEntries([...row.querySelectorAll('[data-detail-field]')].map(input => [input.dataset.detailField, input.dataset.detailField === 'cantidad' ? Number(input.value) : input.value]))) };
        form.dataset.saving = 'true'; fieldset.disabled = true; cancel.disabled = true; setBusy(save, true);
        try {
            const data = await request('ordenes', { method: 'POST', body: payload });
            if (valid(token) && serial === view) { renderOrder(data); setFeedback(el('orders-editor-feedback'), 'Orden y detalles guardados correctamente.', 'success'); }
        } catch (error) {
            if (valid(token) && serial === view) { fieldset.disabled = false; setFeedback(el('orders-editor-feedback'), error.fields?.detalles || error.message, 'error'); showFieldErrors(form, error.fields); }
        } finally { delete form.dataset.saving; fieldset.disabled = false; cancel.disabled = false; setBusy(save, false); }
    });
}
async function showOrder(id) {
    const token = generation, serial = ++view;
    try { const data = await read(`ordenes/${id}`); if (valid(token) && serial === view) renderOrder(data); }
    catch (error) { if (valid(token) && serial === view && error.name !== 'AbortError') setFeedback(el('orders-feedback'), error.message, 'error'); }
}
function renderOrder(data) {
    const order = data.orden, container = editor(`Orden #${order.idOrden}`), token = generation, serial = view;
    const summary = document.createElement('dl'); summary.className = 'order-summary';
    for (const [label, value] of [['Paciente', order.paciente.nombre], ['Cita', `#${order.idCita}`], ['Tipo', types[order.tipoOrden]], ['Estado', states[order.estado]], ['Fecha', order.fechaOrden], ['Observaciones', data.observaciones]]) {
        const term = document.createElement('dt'), description = document.createElement('dd'); term.textContent = label; description.textContent = value; summary.append(term, description);
    }
    container.append(summary);
    const heading = document.createElement('h3'); heading.textContent = 'Detalles'; container.append(heading);
    const list = document.createElement('ol'); list.className = 'order-detail-list';
    data.detalles.forEach(detail => { const row = document.createElement('li'); const description = document.createElement('p'); description.textContent = `${detail.descripcion} · Cantidad: ${detail.cantidad}`; row.append(description); if (detail.indicaciones) { const notes = document.createElement('p'); notes.textContent = `Indicaciones: ${detail.indicaciones}`; row.append(notes); } list.append(row); }); container.append(list);
    const actions = document.createElement('div'); actions.className = 'form-actions'; actions.append(button('Volver al listado', back));
    if (order.estado === 'PENDIENTE') for (const state of ['COMPLETADA', 'CANCELADA']) {
        const action = button(state === 'COMPLETADA' ? 'Marcar completada' : 'Cancelar orden', async () => {
            if (state === 'CANCELADA' && !await confirmAction({ title: 'Cancelar orden', message: 'La orden quedará cancelada y no podrá reabrirse.', confirmLabel: 'Cancelar orden' })) return;
            if (!valid(token) || serial !== view || action.disabled) return;
            actions.querySelectorAll('button').forEach(item => { item.disabled = true; });
            try {
                const updated = await request(`ordenes/${order.idOrden}/estado`, { method: 'PATCH', body: { estado: state } });
                if (valid(token) && serial === view) { renderOrder(updated); setFeedback(el('orders-editor-feedback'), 'Estado de la orden actualizado.', 'success'); }
            } catch (error) { if (valid(token) && serial === view) setFeedback(el('orders-editor-feedback'), error.message, 'error'); }
            finally { actions.querySelectorAll('button').forEach(item => { item.disabled = false; }); }
        }); actions.append(action);
    }
    container.append(actions);
}
el('orders-new').addEventListener('click', createOrder);
el('orders-filters').addEventListener('submit', event => { event.preventDefault(); page = 1; setFeedback(el('orders-feedback')); loadList(); });
el('orders-parent').addEventListener('change', () => { page = 1; loadList(); });
async function searchReferences() { const token = generation; try { await loadReferences(); } catch (error) { if (valid(token) && error.name !== 'AbortError') setFeedback(el('orders-feedback'), error.message, 'error'); } }
el('orders-source-search').addEventListener('click', searchReferences);
el('orders-source-query').addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); searchReferences(); } });
el('orders-retry').addEventListener('click', () => openOrders(new URLSearchParams(location.hash.split('?')[1] || '')));
el('orders-previous').addEventListener('click', () => { if (page > 1) { page--; loadList(); } });
el('orders-next').addEventListener('click', () => { if (page * 20 < total) { page++; loadList(); } });
