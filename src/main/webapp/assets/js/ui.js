/** Utilidades compartidas para los formularios de los próximos módulos. */
export function setFeedback(element, message = '', type = 'info') {
    const allowedType = ['info', 'error', 'success'].includes(type) ? type : 'info';
    element.className = `notice notice-${allowedType}`;
    element.setAttribute('role', allowedType === 'error' ? 'alert' : 'status');
    element.textContent = message;
    element.hidden = !message;
}

export function setBusy(button, busy, label = 'Guardando…') {
    if (busy && !button.hasAttribute('data-original-label')) {
        button.dataset.originalLabel = button.textContent;
        button.dataset.originalDisabled = String(button.disabled);
    }
    button.setAttribute('aria-busy', String(busy));
    if (busy) {
        button.disabled = true;
        button.textContent = label;
    } else if (button.hasAttribute('data-original-label')) {
        button.disabled = button.dataset.originalDisabled === 'true';
        button.textContent = button.dataset.originalLabel;
        delete button.dataset.originalLabel;
        delete button.dataset.originalDisabled;
    }
}

function fields(form) {
    return [...form.elements].filter(field => field.matches('input:not([type="hidden"]), select, textarea') && field.willValidate);
}
export function clearFieldErrors(form) {
    fields(form).forEach(field => {
        field.removeAttribute('aria-invalid');
        const errorId = field.dataset.errorId;
        if (errorId) {
            document.getElementById(errorId)?.remove();
            const ids = (field.getAttribute('aria-describedby') || '').split(/\s+/).filter(id => id && id !== errorId);
            if (ids.length) field.setAttribute('aria-describedby', ids.join(' '));
            else field.removeAttribute('aria-describedby');
            delete field.dataset.errorId;
        }
    });
}
function fieldError(field, message) {
    if (!field.id) field.id = `field-${crypto.randomUUID()}`;
    const error = document.createElement('span');
    error.id = `${field.id}-error`;
    error.className = 'field-error';
    error.textContent = message;
    field.insertAdjacentElement('afterend', error);
    field.dataset.errorId = error.id;
    field.setAttribute('aria-invalid', 'true');
    const ids = new Set((field.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean));
    ids.add(error.id);
    field.setAttribute('aria-describedby', [...ids].join(' '));
}
export function validateForm(form) {
    clearFieldErrors(form);
    const invalid = fields(form).filter(field => !field.validity.valid || (field.required && !field.value.trim()));
    invalid.forEach(field => {
        let message = 'Revisa el valor ingresado.';
        if (field.validity.valueMissing || (field.required && !field.value.trim())) message = 'Completa este campo.';
        else if (field.validity.typeMismatch && field.type === 'email') message = 'Ingresa un correo electrónico válido.';
        else if (field.validity.tooLong) message = `Usa como máximo ${field.maxLength} caracteres.`;
        else if (field.validity.rangeUnderflow) message = `El valor mínimo es ${field.min}.`;
        else if (field.validity.rangeOverflow) message = `El valor máximo es ${field.max}.`;
        else if (field.validity.stepMismatch) message = 'Ingresa un valor con el incremento permitido.';
        fieldError(field, message);
    });
    invalid[0]?.focus();
    return invalid.length === 0;
}
export function showFieldErrors(form, errors = {}) {
    clearFieldErrors(form);
    let first;
    fields(form).forEach(field => {
        const message = Object.hasOwn(errors, field.name) ? errors[field.name] : null;
        if (typeof message === 'string' && message) {
            fieldError(field, message);
            first ||= field;
        }
    });
    first?.focus();
}

export function confirmAction({ title = 'Confirmar operación', message = '¿Deseas continuar?', confirmLabel = 'Confirmar' } = {}) {
    const previousFocus = document.activeElement;
    const dialog = document.createElement('dialog');
    dialog.className = 'confirmation';
    const heading = document.createElement('h2');
    heading.id = `confirmation-${crypto.randomUUID()}`;
    heading.textContent = title;
    dialog.setAttribute('aria-labelledby', heading.id);
    const description = document.createElement('p');
    description.id = `${heading.id}-description`;
    description.textContent = message;
    dialog.setAttribute('aria-describedby', description.id);
    const actions = document.createElement('form');
    actions.method = 'dialog';
    actions.className = 'form-actions';
    const cancel = document.createElement('button');
    cancel.className = 'button button-secondary';
    cancel.value = 'cancel';
    cancel.textContent = 'Cancelar';
    cancel.autofocus = true;
    const confirm = document.createElement('button');
    confirm.className = 'button button-danger';
    confirm.value = 'confirm';
    confirm.textContent = confirmLabel;
    actions.append(cancel, confirm);
    dialog.append(heading, description, actions);
    document.body.append(dialog);
    return new Promise(resolve => {
        dialog.addEventListener('close', () => {
            const accepted = dialog.returnValue === 'confirm';
            dialog.remove();
            previousFocus?.focus();
            resolve(accepted);
        }, { once: true });
        dialog.showModal();
    });
}
