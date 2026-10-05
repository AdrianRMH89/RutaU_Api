package com.rutau.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// US11 - Validación simple de "ruta": Lima se divide en sectores. Un punto intermedio
// coincide con la ruta si su distrito está en el mismo sector que la zona del viaje
// o en un sector vecino. (Sin mapas ni coordenadas: suficiente para el MVP.)
public final class LimaZones {

    private static final Map<String, String> SECTOR_BY_DISTRICT = new HashMap<>();
    private static final Map<String, Set<String>> NEIGHBOURS = Map.of(
            "LIMA_MODERNA", Set.of("LIMA_CENTRO", "LIMA_ESTE", "LIMA_SUR", "CALLAO"),
            "LIMA_CENTRO", Set.of("LIMA_MODERNA", "LIMA_NORTE", "LIMA_ESTE", "CALLAO"),
            "LIMA_NORTE", Set.of("LIMA_CENTRO", "LIMA_ESTE", "CALLAO"),
            "LIMA_ESTE", Set.of("LIMA_CENTRO", "LIMA_MODERNA", "LIMA_NORTE", "LIMA_SUR"),
            "LIMA_SUR", Set.of("LIMA_MODERNA", "LIMA_ESTE"),
            "CALLAO", Set.of("LIMA_NORTE", "LIMA_CENTRO", "LIMA_MODERNA"));

    static {
        add("LIMA_MODERNA", "Miraflores", "San Isidro", "San Borja", "Surco", "Santiago de Surco",
                "La Molina", "Barranco", "Surquillo", "Jesus Maria", "Lince", "Magdalena",
                "Magdalena del Mar", "Pueblo Libre", "San Miguel");
        add("LIMA_CENTRO", "Lima", "Cercado de Lima", "Brena", "La Victoria", "Rimac", "San Luis");
        add("LIMA_NORTE", "Comas", "Los Olivos", "San Martin de Porres", "Independencia",
                "Carabayllo", "Puente Piedra", "Ancon", "Santa Rosa");
        add("LIMA_ESTE", "Ate", "Santa Anita", "El Agustino", "San Juan de Lurigancho",
                "Lurigancho", "Chaclacayo", "Cieneguilla");
        add("LIMA_SUR", "Chorrillos", "San Juan de Miraflores", "Villa Maria del Triunfo",
                "Villa El Salvador", "Lurin", "Pachacamac");
        add("CALLAO", "Callao", "Bellavista", "La Perla", "La Punta", "Carmen de la Legua", "Ventanilla");
    }

    private LimaZones() {}

    private static void add(String sector, String... districts) {
        for (String d : List.of(districts)) {
            SECTOR_BY_DISTRICT.put(normalize(d), sector);
        }
    }

    // "Jesús María " -> "JESUS MARIA" (sin tildes, sin espacios extra, en mayúsculas)
    public static String normalize(String text) {
        String noAccents = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.replaceAll("\\s+", " ").toUpperCase();
    }

    // ¿El distrito del punto intermedio queda dentro de la ruta del viaje?
    public static boolean isOnRoute(String tripZone, String stopZone) {
        String a = normalize(tripZone);
        String b = normalize(stopZone);
        if (a.equals(b)) {
            return true;
        }
        String sectorTrip = SECTOR_BY_DISTRICT.get(a);
        String sectorStop = SECTOR_BY_DISTRICT.get(b);
        if (sectorTrip == null || sectorStop == null) {
            return false;   // distrito desconocido: no se puede asegurar que esté en la ruta
        }
        return sectorTrip.equals(sectorStop) || NEIGHBOURS.get(sectorTrip).contains(sectorStop);
    }
}
