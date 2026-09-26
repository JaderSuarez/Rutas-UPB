package modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Anotación de una acción de bloqueo o desbloqueo hecha por un administrador. */
public class RegistroBloqueo {

    private static final DateTimeFormatter FORMATO_LEGIBLE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDateTime fecha;
    private final String usuario;
    private final String origenId;
    private final String destinoId;
    private final boolean bloqueo; // true = bloqueó, false = desbloqueó

    public RegistroBloqueo(LocalDateTime fecha, String usuario,
                           String origenId, String destinoId, boolean bloqueo) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.bloqueo = bloqueo;
    }

    public LocalDateTime getFecha() { return fecha; }
    public String getUsuario() { return usuario; }
    public String getOrigenId() { return origenId; }
    public String getDestinoId() { return destinoId; }
    public boolean esBloqueo() { return bloqueo; }

    public String getFechaLegible() { return fecha.format(FORMATO_LEGIBLE); }

    public String getAccionLegible() { return bloqueo ? "Bloqueó" : "Desbloqueó"; }

    public String getTramo() { return origenId + " - " + destinoId; }
}
