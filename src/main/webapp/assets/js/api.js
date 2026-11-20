/** Cliente REST relativo al contexto de despliegue, sin mostrar respuestas internas. */
export class ApiRequestError extends Error {
    constructor(message, status = 0, code = 'NETWORK_ERROR', fields = {}) {
        super(message);
        this.name = 'ApiRequestError';
        this.status = status;
        this.code = code;
        this.fields = fields;
    }
}

export async function request(path, options = {}) {
    const base = new URL('resources/', new URL('../../', import.meta.url));
    const url = new URL(String(path).replace(/^\/+/, ''), base);
    if (!url.href.startsWith(base.href)) throw new TypeError('La ruta debe pertenecer a la API del sistema.');
    const headers = new Headers(options.headers);
    headers.set('Accept', 'application/json');
    let body = options.body;
    if (body != null && !(body instanceof FormData) && typeof body !== 'string') {
        headers.set('Content-Type', 'application/json');
        body = JSON.stringify(body);
    }
    let response;
    try {
        response = await fetch(url, { ...options, body, headers, credentials: 'same-origin' });
    } catch (error) {
        if (error.name === 'AbortError') throw error;
        throw new ApiRequestError('No se pudo conectar con el sistema. Revisa tu conexión e inténtalo de nuevo.');
    }
    const json = response.headers.get('content-type')?.includes('application/json');
    let data = null;
    if (response.status !== 204) {
        try { data = json ? await response.json() : await response.text(); }
        catch { throw new ApiRequestError('No se pudo interpretar la respuesta del sistema.', response.status, 'INVALID_RESPONSE'); }
    }
    if (!response.ok) {
        const standardized = json && data && typeof data.code === 'string' && typeof data.message === 'string';
        throw new ApiRequestError(
            standardized ? data.message : 'No se pudo completar la operación. Inténtalo de nuevo.',
            response.status,
            standardized ? data.code : 'REQUEST_ERROR',
            standardized && data.fields && typeof data.fields === 'object' && !Array.isArray(data.fields) ? data.fields : {}
        );
    }
    return data;
}
