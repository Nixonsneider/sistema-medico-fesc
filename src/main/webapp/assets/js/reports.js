import { request } from './api.js';
import { setFeedback, setBusy, validateForm, showFieldErrors } from './ui.js';
const el = id => document.getElementById(id);
const names = { agenda: 'Cumplimiento de agenda', productividad: 'Productividad médica', demanda: 'Demanda horaria', morbilidad: 'Diagnósticos más frecuentes', demografia: 'Demografía', ordenes: 'Órdenes por tipo y médico', cumplimiento: 'Cumplimiento de órdenes', servicios: 'Servicios y medicamentos', historia: 'Resumen individual de historia clínica' };
let token = 0, sequence = 0, controller, snapshot = null, page = 1, total = 0;
export function closeReports() { token++; sequence++; controller?.abort(); }
export function openReports() { closeReports(); controller = new AbortController(); snapshot = null; setBusy(el('reports-generate'), false); el('reports-results').hidden = true; setFeedback(el('reports-feedback')); el('reports-filters').reset(); el('reports-patient').required = false; }
function field(container, name, label, tag = 'input') {
    const div = document.createElement('div'); div.className = 'field'; const caption = document.createElement('label'), input = document.createElement(tag);
    input.id = `reports-${name}`; input.name = name; caption.htmlFor = input.id; caption.textContent = label; div.append(caption, input); container.append(div); return input;
}
function options(select, values, empty = 'Todos') { select.replaceChildren(); const item = document.createElement('option'); item.value = ''; item.textContent = empty; select.append(item); Object.entries(values).forEach(([value, label]) => { const o = document.createElement('option'); o.value = value; o.textContent = label; select.append(o); }); }
const root = el('reports-controls');
const type = field(root, 'tipo', 'Reporte', 'select'); options(type, names, 'Selecciona un reporte'); type.required = true; type.value = 'agenda';
field(root, 'desde', 'Desde (opcional)').type = 'date'; field(root, 'hasta', 'Hasta (opcional)').type = 'date';
options(field(root, 'estadoCita', 'Estado de cita (opcional)', 'select'), { PROGRAMADA: 'Programada', CONFIRMADA: 'Confirmada', ATENDIDA: 'Atendida', CANCELADA: 'Cancelada', NO_ASISTIO: 'No asistió' });
options(field(root, 'estadoOrden', 'Estado de orden (opcional)', 'select'), { PENDIENTE: 'Pendiente', COMPLETADA: 'Completada', CANCELADA: 'Cancelada' });
for (const [name, label, endpoint] of [['idMedico', 'Médico', 'medicos'], ['idEspecialidad', 'Especialidad', 'especialidades'], ['idPaciente', 'Paciente (obligatorio para historia)', 'pacientes']]) {
    const select = field(root, name, label, 'select'); if (name === 'idPaciente') select.id = 'reports-patient';
    select.previousElementSibling.htmlFor = select.id; options(select, {});
    const search = document.createElement('input'); search.type = 'search'; search.maxLength = 100; search.setAttribute('aria-label', `Buscar ${label}`);
    const button = document.createElement('button'); button.type = 'button'; button.className = 'button button-secondary'; button.textContent = 'Buscar opciones';
    const wrapper = document.createElement('div'); wrapper.className = 'lookup-search'; wrapper.append(search, button); select.parentElement.append(wrapper);
    let serial = 0;
    button.addEventListener('click', async () => {
        const current = token, requestId = ++serial; setBusy(button, true, 'Buscando…');
        try {
            const data = await request(`${endpoint}${endpoint === 'especialidades' ? '?pageSize=100&' : '/opciones?'}q=${encodeURIComponent(search.value.trim())}`, { signal: controller.signal });
            if (current !== token || requestId !== serial) return;
            const values = {}; (data.items || data).forEach(item => { values[item.id || item.idEspecialidad] = endpoint === 'especialidades' ? item.nombre : `${item.nombre} · ${item.documento}`; }); options(select, values); invalidate();
        } catch (error) { if (current === token && error.name !== 'AbortError') setFeedback(el('reports-feedback'), error.message, 'error'); }
        finally { setBusy(button, false); }
    });
    search.addEventListener('keydown', event => { if (event.key === 'Enter') { event.preventDefault(); button.click(); } });
}
function invalidate() { sequence++; snapshot = null; el('reports-results').hidden = true; setFeedback(el('reports-feedback'), 'Pulsa Generar para consultar con los filtros seleccionados.'); }
type.addEventListener('change', () => { el('reports-patient').required = type.value === 'historia'; });
el('reports-filters').addEventListener('change', invalidate);
el('reports-filters').addEventListener('submit', event => {
    event.preventDefault(); if (!validateForm(event.currentTarget)) return;
    snapshot = new URLSearchParams([...new FormData(event.currentTarget)].filter(([, value]) => value !== '')); page = 1; load();
});
async function load() {
    const current = token, serial = ++sequence, filters = new URLSearchParams(snapshot); filters.set('page', page); filters.set('pageSize', '20');
    setBusy(el('reports-generate'), true, 'Generando…'); el('reports-results').hidden = true; setFeedback(el('reports-feedback'));
    try {
        const data = await request(`reportes?${filters}`, { signal: controller.signal });
        if (current !== token || serial !== sequence) return;
        total = data.total; el('reports-results').hidden = false; el('reports-result-title').textContent = data.titulo;
        el('reports-scope').textContent = data.alcance;
        el('reports-kpis').replaceChildren(); Object.entries(data.indicadores).forEach(([label, value]) => { const div = document.createElement('div'); const title = document.createElement('span'), number = document.createElement('strong'); title.textContent = label; number.textContent = value; div.append(title, number); el('reports-kpis').append(div); });
        el('reports-chart').replaceChildren(); const max = Math.max(1, ...data.grafico.map(bar => bar.cantidad));
        data.grafico.forEach(bar => { const row = document.createElement('div'), label = document.createElement('span'), meter = document.createElement('meter'); label.textContent = `${bar.etiqueta}: ${bar.cantidad}`; meter.min = 0; meter.max = max; meter.value = bar.cantidad; meter.setAttribute('aria-label', label.textContent); row.append(label, meter); el('reports-chart').append(row); });
        el('reports-chart-empty').hidden = data.grafico.length > 0;
        el('reports-columns').replaceChildren(); data.columnas.forEach(text => { const th = document.createElement('th'); th.scope = 'col'; th.textContent = text; el('reports-columns').append(th); });
        el('reports-rows').replaceChildren(); data.filas.forEach(values => { const tr = document.createElement('tr'); values.forEach(text => { const td = document.createElement('td'); td.textContent = text; tr.append(td); }); el('reports-rows').append(tr); });
        el('reports-empty').hidden = data.filas.length > 0;
        el('reports-page').textContent = `${total} filas · Página ${page} de ${Math.max(1, Math.ceil(total / 20))}`;
        el('reports-previous').disabled = page <= 1; el('reports-next').disabled = page * 20 >= total;
    } catch (error) { if (current === token && serial === sequence && error.name !== 'AbortError') { setFeedback(el('reports-feedback'), error.message, 'error'); showFieldErrors(el('reports-filters'), error.fields); } }
    finally { if (current === token) setBusy(el('reports-generate'), false); }
}
el('reports-previous').addEventListener('click', () => { if (snapshot && page > 1) { page--; load(); } });
el('reports-next').addEventListener('click', () => { if (snapshot && page * 20 < total) { page++; load(); } });
for (const button of el('reports-exports').querySelectorAll('button')) button.addEventListener('click', async () => {
    if (!snapshot) return; const current = token, filters = new URLSearchParams(snapshot); filters.set('formato', button.dataset.format); setBusy(button, true, 'Exportando…');
    try {
        const url = new URL(`resources/reportes/exportar?${filters}`, new URL('../../', import.meta.url));
        const response = await fetch(url, { credentials: 'same-origin', signal: controller.signal });
        if (!response.ok) { const error = await response.json(); throw new Error(error.message + (error.fields ? ' ' + Object.values(error.fields).join(' ') : '')); }
        const blob = await response.blob(); if (current !== token) return;
        const href = URL.createObjectURL(blob), anchor = document.createElement('a'); anchor.href = href; anchor.download = `reporte-${filters.get('tipo')}.${button.dataset.format}`; document.body.append(anchor); anchor.click(); anchor.remove(); setTimeout(() => URL.revokeObjectURL(href), 1000);
        setFeedback(el('reports-feedback'), 'Archivo generado. Incluye todas las filas de la selección (máximo 10000).', 'success');
    } catch (error) { if (current === token && error.name !== 'AbortError') setFeedback(el('reports-feedback'), error.message, 'error'); }
    finally { setBusy(button, false); }
});
