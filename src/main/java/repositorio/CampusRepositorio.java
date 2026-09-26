package repositorio;

import modelo.GrafoCampus;
import modelo.Lugar;
import modelo.Edificio;

public class CampusRepositorio {

    public static GrafoCampus cargarGrafo() {
        GrafoCampus grafo = new GrafoCampus();

        // 1. Crear los edificios y puntos de referencia del campus
        // Lugares actualizados según "Mapa de Lugares por Edificio" (Fase 6)

        Edificio mA = new Edificio("A", "Edificio A");
        mA.agregarLugar(new Lugar("L_A1", "Cafetería A tipo buffet", "Comidas"));
        mA.agregarLugar(new Lugar("L_A2", "Coordinación Cultural y Deportes", "Bienestar"));
        mA.agregarLugar(new Lugar("L_A3", "Maestría en Psicología", "Posgrados"));
        mA.agregarLugar(new Lugar("L_A4", "Laboratorio de Psicometría", "Académico"));
        mA.agregarLugar(new Lugar("L_A5", "Cámara Gesell", "Académico"));
        mA.agregarLugar(new Lugar("L_A6", "Posgrados de Psicología", "Posgrados"));
        mA.agregarLugar(new Lugar("L_A7", "Unidad de Emprendimiento", "Administrativo"));

        Edificio mB = new Edificio("B", "Edificio B");
        mB.agregarLugar(new Lugar("L_B1", "Departamento de Ciencias Básicas", "Académico"));
        mB.agregarLugar(new Lugar("L_B4", "La Tienda del Café El Gualilo", "Comidas"));
        mB.agregarLugar(new Lugar("L_B2", "Laboratorio de Física y Química", "Académico"));
        mB.agregarLugar(new Lugar("L_B3", "Sala de Docentes", "Académico"));

        Edificio mC = new Edificio("C", "Edificio C");
        mC.agregarLugar(new Lugar("L_C1", "Banco de Bogotá", "Servicios"));
        mC.agregarLugar(new Lugar("L_C2", "Aulas y Espacios Audiovisuales", "Académico"));
        mC.agregarLugar(new Lugar("L_C3", "Departamento de Compras y Gestión de Activos", "Administrativo"));
        mC.agregarLugar(new Lugar("L_C4", "Punto Bolsa BVC", "Servicios"));

        Edificio mD = new Edificio("D", "Edificio D");
        mD.agregarLugar(new Lugar("L_D1", "Oficina de Objetos Perdidos", "Atención al usuario"));
        mD.agregarLugar(new Lugar("L_D2", "Rectoría", "Administrativo"));
        mD.agregarLugar(new Lugar("L_D3", "Vicerrectorías", "Administrativo"));
        mD.agregarLugar(new Lugar("L_D4", "Secretaría General", "Administrativo"));
        mD.agregarLugar(new Lugar("L_D5", "Comunicaciones y RRPP", "Administrativo"));
        mD.agregarLugar(new Lugar("L_D6", "Dirección de Docencia", "Administrativo"));
        mD.agregarLugar(new Lugar("L_D7", "Dirección de Planeación", "Administrativo"));
        mD.agregarLugar(new Lugar("L_D8", "Escuela de Derecho y Ciencias Políticas", "Académico"));

        Edificio mE = new Edificio("E", "Edificio E");
        mE.agregarLugar(new Lugar("L_E1", "Sala de Música", "Bienestar"));
        mE.agregarLugar(new Lugar("L_E2", "Escuela de Economía, Administración y Negocios", "Académico"));
        mE.agregarLugar(new Lugar("L_E3", "Sala de Docentes", "Académico"));
        mE.agregarLugar(new Lugar("L_E4", "Posgrados Administración y Negocios", "Posgrados"));

        Edificio mF = new Edificio("F", "Edificio F");
        mF.agregarLugar(new Lugar("L_F1", "Cafetería Terraza Café", "Comidas"));
        mF.agregarLugar(new Lugar("L_F2", "Papelería Mi Dulce Papelería", "Servicios"));
        mF.agregarLugar(new Lugar("L_F3", "Sala de Audiencias", "Académico"));
        mF.agregarLugar(new Lugar("L_F4", "Sala de Grupos Culturales / Aulas", "Bienestar"));

        Edificio mG = new Edificio("G", "Edificio G");
        mG.agregarLugar(new Lugar("L_G1", "Departamento de Bienestar Universitario", "Bienestar"));
        mG.agregarLugar(new Lugar("L_G2", "Consultorio Médico y Psicológico", "Bienestar"));
        mG.agregarLugar(new Lugar("L_G3", "Oficina de Egresados", "Administrativo"));
        mG.agregarLugar(new Lugar("L_G4", "Programa de Acompañamiento Académico", "Bienestar"));

        Edificio mH = new Edificio("H", "Edificio H");
        mH.agregarLugar(new Lugar("L_H1", "Auditorio Juan Pablo II", "Eventos"));
        mH.agregarLugar(new Lugar("L_H2", "Escuela de Ciencias Sociales", "Académico"));
        mH.agregarLugar(new Lugar("L_H3", "Departamento de Humanística / Aulas", "Académico"));
        mH.agregarLugar(new Lugar("L_H4", "Facultad de Diseño", "Académico"));

        Edificio mI = new Edificio("I", "Edificio I");
        mI.agregarLugar(new Lugar("L_I1", "Laboratorio de Ingeniería Mecánica", "Investigación"));
        mI.agregarLugar(new Lugar("L_I2", "Laboratorios de Sistemas e Informática", "Investigación"));
        mI.agregarLugar(new Lugar("L_I3", "Aulas", "Académico"));

        Edificio mJ = new Edificio("J", "Edificio J");
        mJ.agregarLugar(new Lugar("L_J1", "Biblioteca Benedicto XVI", "Biblioteca Principal"));
        mJ.agregarLugar(new Lugar("L_J2", "Auditorio menor Monseñor Jesús Quirós Crispín", "Eventos"));
        mJ.agregarLugar(new Lugar("L_J3", "Departamento Dirección Financiera", "Administrativo"));
        mJ.agregarLugar(new Lugar("L_J4", "Departamento Coordinación de Mercadeo y Producción", "Administrativo"));
        mJ.agregarLugar(new Lugar("L_J5", "Coordinación de Admisiones, Registro y Control Académico", "Administrativo"));
        mJ.agregarLugar(new Lugar("L_J6", "Plazoleta del J", "Zona Común"));
        mJ.agregarLugar(new Lugar("L_J7", "Departamento de Promoción Académica", "Administrativo"));
        mJ.agregarLugar(new Lugar("L_J8", "Cafetería J EAT BOX", "Comidas"));

        Edificio mK = new Edificio("K", "Edificio K");
        mK.agregarLugar(new Lugar("L_K1", "Aula Múltiple Luis Alfonso Díaz Nieto", "Eventos"));
        mK.agregarLugar(new Lugar("L_K2", "Cafetería Edificio K", "Comidas"));
        mK.agregarLugar(new Lugar("L_K3", "Facultad de Ingeniería (Ateneo)", "Académico"));
        mK.agregarLugar(new Lugar("L_K4", "Laboratorio de Ingeniería", "Investigación"));
        mK.agregarLugar(new Lugar("L_K5", "Laboratorios de Informática", "Investigación"));
        mK.agregarLugar(new Lugar("L_K6", "Salas de Informática", "Académico"));

        Edificio mL = new Edificio("L", "Edificio L");
        mL.agregarLugar(new Lugar("L_L1", "Cafetería L EAT BOX", "Comidas"));
        mL.agregarLugar(new Lugar("L_L2", "Departamento de Educación Continua", "Administrativo"));
        mL.agregarLugar(new Lugar("L_L3", "Dirección de Investigaciones y Transferencia", "Investigación"));
        mL.agregarLugar(new Lugar("L_L4", "Aulas / Salas de Informática", "Académico"));
        mL.agregarLugar(new Lugar("L_L5", "Centro de Tecnología de Información y Comunicaciones", "Servicios"));
        mL.agregarLugar(new Lugar("L_L6", "Centro de Lenguas", "Académico"));

        Edificio mM = new Edificio("M", "Edificio M");
        mM.agregarLugar(new Lugar("L_M1", "Polideportivo: Canchas Múltiples", "Deportes"));
        mM.agregarLugar(new Lugar("L_M2", "Canchas de Pádel", "Deportes"));
        mM.agregarLugar(new Lugar("L_M3", "Gimnasio", "Deportes"));
        mM.agregarLugar(new Lugar("L_M4", "Sauna", "Deportes"));

        Edificio templo = new Edificio("Templo", "Templo Universitario");
        templo.agregarLugar(new Lugar("L_TEMPLO", "Templo Universitario", "Espiritual"));

        Edificio caf = new Edificio("CAF", "Cafetería EAT BOX (CAF)");
        caf.agregarLugar(new Lugar("L_CAF", "Cafetería EAT BOX", "Comidas"));

        Edificio p1 = new Edificio("Porteria 1", "Portería 1 (Principal)");
        p1.agregarLugar(new Lugar("L_P1", "Portería Principal", "Acceso"));

        Edificio p2 = new Edificio("Porteria 2", "Portería 2 (Secundaria)");
        p2.agregarLugar(new Lugar("L_P2", "Portería Secundaria", "Acceso"));

        // Agregar todos los edificios al grafo
        grafo.agregarEdificio(mA); grafo.agregarEdificio(mB); grafo.agregarEdificio(mC);
        grafo.agregarEdificio(mD); grafo.agregarEdificio(mE); grafo.agregarEdificio(mF);
        grafo.agregarEdificio(mG); grafo.agregarEdificio(mH); grafo.agregarEdificio(mI);
        grafo.agregarEdificio(mJ); grafo.agregarEdificio(mK); grafo.agregarEdificio(mL);
        grafo.agregarEdificio(mM); grafo.agregarEdificio(templo); grafo.agregarEdificio(caf);
        grafo.agregarEdificio(p1); grafo.agregarEdificio(p2);

        // 2. Agregar Caminos (Aristas) con distancias reales (Figura 3 del documento)
        // param: origen, destino, distancia(m), tieneEscaleras
        // Regla de la imagen: línea NEGRA = tieneEscaleras = true (obligatorio subir escaleras)
        //                     línea FUCSIA = tieneEscaleras = false (ruta accesible, sin escaleras)
        grafo.agregarCamino("M", "Porteria 2", 141.0, false);
        grafo.agregarCamino("M", "Porteria 1", 193.0, false);
        grafo.agregarCamino("M", "Templo", 130.0, false);
        grafo.agregarCamino("Porteria 2", "K", 189.0, true);
        grafo.agregarCamino("Porteria 2", "D", 108.0, true);
        grafo.agregarCamino("K", "L", 127.0, false);
        grafo.agregarCamino("K", "I", 109.0, false);
        grafo.agregarCamino("K", "D", 118.0, true);
        grafo.agregarCamino("L", "I", 58.0, false);
        grafo.agregarCamino("L", "H", 134.0, false);
        grafo.agregarCamino("I", "D", 80.0, true);
        grafo.agregarCamino("I", "E", 75.0, true);
        grafo.agregarCamino("I", "H", 140.0, true);
        grafo.agregarCamino("H", "E", 16.0, false);
        grafo.agregarCamino("H", "F", 22.0, false);
        grafo.agregarCamino("H", "G", 53.0, false);
        grafo.agregarCamino("E", "D", 50.0, true);
        grafo.agregarCamino("E", "F", 9.0, false);
        grafo.agregarCamino("E", "Porteria 2", 120.0, true);
        grafo.agregarCamino("F", "D", 60.0, true);
        grafo.agregarCamino("F", "G", 37.0, false);
        grafo.agregarCamino("D", "G", 73.0, false);
        grafo.agregarCamino("D", "CAF", 44.0, true);
        grafo.agregarCamino("D", "C", 62.0, true);
        grafo.agregarCamino("D", "J", 48.0, false);
        grafo.agregarCamino("D", "Porteria 1", 110.0, true);
        grafo.agregarCamino("CAF", "A", 31.0, true);
        grafo.agregarCamino("CAF", "B", 24.0, true);
        grafo.agregarCamino("CAF", "C", 27.0, true);
        grafo.agregarCamino("A", "Templo", 52.0, false);
        grafo.agregarCamino("A", "B", 14.0, false);
        grafo.agregarCamino("B", "Templo", 46.0, false);
        grafo.agregarCamino("B", "C", 13.0, false);
        grafo.agregarCamino("B", "Porteria 1", 54.0, false);
        grafo.agregarCamino("C", "J", 84.0, false);
        grafo.agregarCamino("C", "Porteria 1", 60.0, false);
        grafo.agregarCamino("J", "G", 65.0, false);
        grafo.agregarCamino("J", "Porteria 1", 47.0, false);
        grafo.agregarCamino("Templo", "Porteria 1", 63.0, false);

        return grafo;
    }
}
