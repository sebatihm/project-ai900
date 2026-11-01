package info.rio.project_ai901.services.report.components;


import info.rio.project_ai901.exceptions.InvalidRequestException;
import info.rio.project_ai901.services.report.dto.ReportPeriod;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Component
public class ReportPeriodResolver {

    public static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final Locale ES = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReportPeriod resolve(Integer month, Integer year, LocalDate from, LocalDate to) {
        return resolve(month, year, from, to, LocalDate.now(ZONE));
    }

    ReportPeriod resolve(Integer month, Integer year, LocalDate from, LocalDate to, LocalDate today) {

        // 1) Rango explicito
        if (from != null || to != null) {
            if (from == null || to == null) {
                throw new InvalidRequestException("Para un rango indica la fecha inicial y la final.");
            }
            if (to.isBefore(from)) {
                throw new InvalidRequestException("La fecha final no puede ser anterior a la inicial.");
            }
            if (from.plusMonths(12).isBefore(to)) {
                throw new InvalidRequestException("El rango maximo permitido es de 12 meses.");
            }
            notFuture(from, today);
            return new ReportPeriod(from, to, DMY.format(from) + " al " + DMY.format(to));
        }

        // 2) Solo anio, sin mes: ambiguo
        if (month == null && year != null) {
            throw new InvalidRequestException("Indica el mes del anio solicitado.");
        }

        // 3) Sin nada: mes en curso. Solo mes: ese mes de este anio.
        int y;
        int m;
        if (month == null) {
            m = today.getMonthValue();
            y = today.getYear();
        } else {
            m = month;
            y = (year != null) ? year : today.getYear();
        }
        if (m < 1 || m > 12) {
            throw new InvalidRequestException("El mes debe estar entre 1 y 12.");
        }
        if (y < 2000 || y > today.getYear()) {
            throw new InvalidRequestException("El anio solicitado no es valido.");
        }

        YearMonth ym = YearMonth.of(y, m);
        notFuture(ym.atDay(1), today);
        String name = ym.getMonth().getDisplayName(TextStyle.FULL, ES);
        String label = Character.toUpperCase(name.charAt(0)) + name.substring(1) + " de " + y;
        return new ReportPeriod(ym.atDay(1), ym.atEndOfMonth(), label);
    }

    private void notFuture(LocalDate start, LocalDate today) {
        if (start.isAfter(today)) {
            throw new InvalidRequestException("Ese periodo aun no ocurre, no hay ventas que reportar.");
        }
    }
}