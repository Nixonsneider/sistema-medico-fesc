import { openRecords, closeRecords } from './records.js';
import { openOrders, closeOrders } from './orders.js';
import { openReports, closeReports } from './reports.js';
import { openClinical, closeClinical } from './clinical.js';
import { openDirectory, closeDirectory } from './directory.js';

const modules = {
    especialidades: ['Especialidades', 'Administración', 'Catálogo de especialidades que se asignan a los médicos.'],
    medicos: ['Médicos', 'Administración', 'Información profesional, registro médico, contacto y especialidad.'],
    pacientes: ['Pacientes', 'Administración', 'Identificación, información personal y datos de contacto.'],
    citas: ['Citas', 'Administración', 'Agenda de atención con el paciente, el médico y el estado de cada cita.'],
    historias: ['Historias clínicas', 'Atención clínica', 'Antecedentes, alergias y grupo sanguíneo de cada paciente.'],
    diagnosticos: ['Diagnósticos', 'Atención clínica', 'Diagnósticos y códigos CIE-10 asociados a una cita.'],
    tratamientos: ['Tratamientos', 'Atención clínica', 'Medicamentos, dosis, frecuencia, duración e indicaciones.'],
    ordenes: ['Órdenes médicas', 'Atención clínica', 'Órdenes de laboratorio, imagenología, medicamento o remisión y sus detalles.'],
    reportes: ['Reportes', 'Consultas', 'Información operativa y clínica para consultar la actividad del consultorio.']
};

const sidebar = document.querySelector('#navegacion');
const menu = document.querySelector('#menu-toggle');
const main = document.querySelector('#contenido');
function closeMenu() {
    sidebar.classList.remove('is-open');
    menu.setAttribute('aria-expanded', 'false');
}
function route(focus = false) {
    const [routeKey, routeQuery = ''] = location.hash.slice(1).split('?');
    const key = routeKey || 'inicio';
    if (key === 'contenido') return;
    const home = key === 'inicio';
    const directory = key === 'especialidades' || key === 'medicos';
    const clinical = ['pacientes', 'historias', 'citas'].includes(key);
    const records = ['diagnosticos', 'tratamientos'].includes(key);
    const orders = key === 'ordenes';
    const reports = key === 'reportes';
    document.querySelector('#home-view').hidden = !home;
    document.querySelector('#module-view').hidden = home || directory || clinical || records || orders || reports;
    document.querySelector('#reports-view').hidden = !reports;
    if (reports) openReports(); else closeReports();
    document.querySelector('#orders-view').hidden = !orders;
    if (orders) openOrders(new URLSearchParams(routeQuery));
    else closeOrders();
    document.querySelector('#records-view').hidden = !records;
    if (records) openRecords(key, new URLSearchParams(routeQuery));
    else closeRecords();
    document.querySelector('#clinical-view').hidden = !clinical;
    if (clinical) openClinical(key, new URLSearchParams(routeQuery));
    else closeClinical();
    document.querySelector('#directory-view').hidden = !directory;
    if (directory) openDirectory(key);
    else closeDirectory();
    const module = modules[key];
    if (!home) {
        document.querySelector('#module-title').textContent = module?.[0] || 'Página no encontrada';
        document.querySelector('#module-category').textContent = module?.[1] || 'Navegación';
        document.querySelector('#module-description').textContent = module?.[2] || 'La sección solicitada no está disponible.';
        document.querySelector('#module-state').hidden = !module;
        document.querySelector('#module-state').textContent = key === 'reportes' ? 'Pendiente' : 'En preparación';
        document.querySelector('#module-notice-title').textContent = module ? 'Este módulo estará disponible próximamente' : 'Revisa la dirección o vuelve al inicio';
        document.querySelector('#module-notice').textContent = module ? 'Todavía no es posible registrar ni consultar información en esta sección.' : 'Utiliza el menú para acceder a las secciones del sistema.';
    }
    document.title = `${home ? 'Inicio' : module?.[0] || 'Página no encontrada'} | Sistema Médico`;
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.hash === `#${key}`) link.setAttribute('aria-current', 'page');
        else link.removeAttribute('aria-current');
    });
    closeMenu();
    if (focus) main.focus();
}
menu.addEventListener('click', () => {
    const open = sidebar.classList.toggle('is-open');
    menu.setAttribute('aria-expanded', String(open));
});
document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && sidebar.classList.contains('is-open')) {
        closeMenu();
        menu.focus();
    }
});
window.matchMedia('(max-width: 700px)').addEventListener('change', closeMenu);
window.addEventListener('hashchange', () => route(true));
document.querySelector('.skip-link').addEventListener('click', () => main.focus());

const search = document.querySelector('#module-search');
const normalize = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es');
search.addEventListener('input', () => {
    const query = normalize(search.value.trim());
    let count = 0;
    document.querySelectorAll('#module-rows tr').forEach(row => {
        const matches = normalize(row.cells[0].textContent + ' ' + row.cells[1].textContent).includes(query);
        row.hidden = !matches;
        if (matches) count++;
    });
    document.querySelector('#search-empty').hidden = count !== 0;
    document.querySelector('#search-summary').textContent = `${count} ${count === 1 ? 'módulo' : 'módulos'}`;
});
route();
