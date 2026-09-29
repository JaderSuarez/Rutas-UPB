package movil;

import modelo.Usuario;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

/**
 * Inicio de sesión, registro y acceso de administrador, con las mismas reglas
 * que las ventanas de escritorio (VentanaLogin, VentanaRegistro y VentanaLoginAdmin).
 */
final class PantallaAcceso {

    private final AppMovil app;
    private final HTMLElement elemento = Dom.el("div", "acceso");
    private final HTMLElement tarjeta = Dom.el("div", "tarjeta tarjeta-acceso");
    private HTMLElement error;

    PantallaAcceso(AppMovil app) {
        this.app = app;
        HTMLElement portada = Dom.el("div", "acceso-portada");
        portada.appendChild(Dom.texto("div", "escudo-grande", "UPB"));
        portada.appendChild(Dom.texto("h1", null, "Rutas UPB"));
        portada.appendChild(Dom.texto("p", null, "Encuentra la mejor ruta dentro del campus"));
        elemento.appendChild(portada);
        elemento.appendChild(tarjeta);
        elemento.appendChild(Dom.texto("p", "acceso-pie", "Universidad Pontificia Bolivariana · Seccional Bucaramanga"));
        HTMLElement escritorio = Dom.el("p", "version-escritorio");
        HTMLElement enlace = Dom.texto("a", null, "Abrir la versión de escritorio");
        enlace.setAttribute("href", "../?escritorio");
        escritorio.appendChild(enlace);
        elemento.appendChild(escritorio);
        mostrarLogin(false);
    }

    HTMLElement getElemento() {
        return elemento;
    }

    private HTMLElement nuevoError() {
        error = Dom.el("p", "mensaje-error");
        return error;
    }

    private void mostrarError(String mensaje) {
        error.setClassName("mensaje-error");
        error.setTextContent(mensaje);
    }

    private void mostrarExito(String mensaje) {
        error.setClassName("mensaje-exito");
        error.setTextContent(mensaje);
    }

    // ==================== Inicio de sesión ====================

    private void mostrarLogin(boolean administrador) {
        Dom.vaciar(tarjeta);
        tarjeta.appendChild(Dom.texto("h2", null, administrador ? "Acceso administrador" : "Iniciar sesión"));
        if (administrador) {
            tarjeta.appendChild(Dom.texto("p", "texto-suave",
                    "Solo para cuentas con permisos de administración."));
        }

        HTMLInputElement correo = Dom.campo("email", "nombre.apellido@upb.edu.co");
        correo.setAttribute("autocomplete", "username");
        HTMLInputElement clave = Dom.campo("password", "Contraseña");
        clave.setAttribute("autocomplete", "current-password");
        tarjeta.appendChild(Dom.grupo("Correo institucional", correo));
        tarjeta.appendChild(Dom.grupo("Contraseña", clave));
        tarjeta.appendChild(nuevoError());

        Runnable entrar = () -> autenticar(correo.getValue().trim(), clave.getValue(), administrador);
        tarjeta.appendChild(Dom.boton("Ingresar", "btn btn-primario btn-bloque", entrar));
        clave.addEventListener("keydown", e -> {
            if ("Enter".equals(((org.teavm.jso.dom.events.KeyboardEvent) e).getKey())) entrar.run();
        });

        if (administrador) {
            tarjeta.appendChild(Dom.boton("Volver al inicio de sesión", "btn btn-texto btn-bloque",
                    () -> mostrarLogin(false)));
        } else {
            tarjeta.appendChild(Dom.boton("Crear cuenta", "btn btn-contorno btn-bloque", this::mostrarRegistro));
            tarjeta.appendChild(Dom.boton("Entrar como invitado", "btn btn-dorado btn-bloque", () -> {
                app.controlador.iniciarSesionInvitado();
                app.entrar(null);
            }));
            tarjeta.appendChild(Dom.boton("Acceso administrador", "btn btn-texto btn-bloque",
                    () -> mostrarLogin(true)));
        }
    }

    private void autenticar(String correo, String clave, boolean administrador) {
        if (correo.isEmpty() || clave.isEmpty()) {
            mostrarError("Por favor ingresa correo y contraseña.");
            return;
        }
        if (!correo.contains("@")) {
            mostrarError("Ingresa un correo válido.");
            return;
        }
        Usuario u = app.controlador.autenticar(correo, clave);
        app.persistir();   // por si la cuenta se actualizó al esquema cifrado
        if (u == null) {
            mostrarError("Correo o contraseña incorrectos.");
            return;
        }
        if (administrador && !u.esAdministrador()) {
            app.controlador.cerrarSesion();
            mostrarError("Esta cuenta no tiene permisos de administrador.");
            return;
        }
        if (!administrador && u.esAdministrador()) {
            app.controlador.cerrarSesion();
            mostrarError("Esta es una cuenta de administrador: ingresa por \"Acceso administrador\".");
            return;
        }
        app.entrar(u);
    }

    // ==================== Registro ====================

    private void mostrarRegistro() {
        Dom.vaciar(tarjeta);
        tarjeta.appendChild(Dom.texto("h2", null, "Crear cuenta"));
        tarjeta.appendChild(Dom.texto("p", "texto-suave", "Debes usar tu correo UPB (@upb.edu.co)."));

        HTMLInputElement nombre = Dom.campo("text", "Tu nombre completo");
        HTMLInputElement correo = Dom.campo("email", "nombre.apellido@upb.edu.co");
        HTMLInputElement clave = Dom.campo("password", "Mínimo 5 caracteres");
        clave.setAttribute("autocomplete", "new-password");
        HTMLSelectElement rol = Dom.lista("campo");
        Dom.opcion(rol, "ESTUDIANTE", "Estudiante");
        Dom.opcion(rol, "EMPLEADO", "Empleado");

        tarjeta.appendChild(Dom.grupo("Nombre", nombre));
        tarjeta.appendChild(Dom.grupo("Correo institucional", correo));
        tarjeta.appendChild(Dom.grupo("Contraseña", clave));
        tarjeta.appendChild(Dom.grupo("Soy", rol));
        tarjeta.appendChild(nuevoError());

        tarjeta.appendChild(Dom.boton("Crear cuenta", "btn btn-primario btn-bloque", () -> {
            String n = nombre.getValue().trim(), c = correo.getValue().trim(), p = clave.getValue();
            if (n.isEmpty()) {
                mostrarError("Ingresa tu nombre completo.");
            } else if (!c.toLowerCase().endsWith("@upb.edu.co")) {
                mostrarError("Debe ser un correo institucional @upb.edu.co");
            } else if (p.length() < 5) {
                mostrarError("La contraseña debe tener al menos 5 caracteres.");
            } else if (app.controlador.existeCorreo(c)) {
                mostrarError("Ese correo ya está registrado. Inicia sesión.");
            } else if (app.controlador.registrarUsuario(n, c, p, Usuario.Rol.desdeTexto(rol.getValue()))) {
                app.persistir();
                mostrarLogin(false);
                mostrarExito("Cuenta creada. Ya puedes iniciar sesión.");
            } else {
                mostrarError("No se pudo guardar la cuenta.");
            }
        }));
        tarjeta.appendChild(Dom.boton("Ya tengo cuenta", "btn btn-texto btn-bloque", () -> mostrarLogin(false)));
    }
}
