package com.stipend.repository;

/**
 * ABSTRACTION: Exportable defines a contract for any object that can
 * produce a CSV row.  Transaction subclasses and MonthlyReport both
 * implement this interface.  ExportService deals only with Exportable —
 * it never needs to know the concrete type.
 */
public interface Exportable {

    /** Returns the CSV header line for this type of data. */
    String getCsvHeader();

    /** Returns one CSV row representing this object's data. */
    String toCsvRow();
}
