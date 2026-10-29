package com.hmmi.guessnumber;

public final class DuelPointCalculator {

    private DuelPointCalculator() {
        // Utility class
    }

    public static long calculatePoint(
            boolean firstTimeOpponent,
            boolean sameDevice,
            boolean sameIp,
            int sameOpponentCount,
            long daysSinceLastOpponentMatch,
            long daysSinceLastSameDeviceMatch,
            long daysSinceLastSameIpMatch
    ) {

        if (firstTimeOpponent && !sameDevice && !sameIp) {
            return 1000L;
        }

        sameOpponentCount = Math.max(1, sameOpponentCount);
        daysSinceLastOpponentMatch = Math.max(1, daysSinceLastOpponentMatch);
        daysSinceLastSameDeviceMatch = Math.max(1, daysSinceLastSameDeviceMatch);
        daysSinceLastSameIpMatch = Math.max(1, daysSinceLastSameIpMatch);

        // U: Aynı rakiple sık maç yapma cezası (Sık maç yapıldıkça çarpan düşer)
        double U = Math.max(
                0.10,
                0.90 - ((sameOpponentCount - 1) * 0.15)
        );

        // Z: Aynı rakiple aradan geçen süre bonusu (Zaman geçtikçe çarpan tekrar artar)
        double Z = 0.10 + (0.80 * ((double) daysSinceLastOpponentMatch / (daysSinceLastOpponentMatch + 7.0)));

        // D: Aynı cihaz kontrolü
        double D;
        if (!sameDevice) {
            D = 0.90;
        } else {
            D = 0.10 + (0.80 * ((double) daysSinceLastSameDeviceMatch / (daysSinceLastSameDeviceMatch + 14.0)));
        }

        // IP: Aynı IP adresi kontrolü
        double IP;
        if (!sameIp) {
            IP = 0.90;
        } else {
            IP = 0.10 + (0.80 * ((double) daysSinceLastSameIpMatch / (daysSinceLastSameIpMatch + 10.0)));
        }

        // Nihai puan hesaplama
        double point = 1000.0 * U * Z * D * IP;

        return Math.max(1L, Math.round(point));
    }
}