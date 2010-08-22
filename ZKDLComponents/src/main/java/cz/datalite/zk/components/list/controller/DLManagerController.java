package cz.datalite.zk.components.list.controller;

import java.util.List;

/**
 * Interface for the listbox component which provides
 * many utils and tools for the combobox.
 * @author Karel Čemus <cemus@datalite.cz>
 */
public interface DLManagerController {

    /**
     * Invokes column manager which allows user to
     * change order and visibility of the cols.
     */
    void onColumnManager();

    /**
     * Invokes sort manager which allows user to create more
     * sorting criterias than one.
     */
    void onSortManager();

    /**
     * Invokes filter manager which allows user to define
     * many filters.
     */
    void onFilterManager();

    /**
     * Invokes export manager which allows user to export
     * datas to the excel file.
     */
    void onExportManager();

    /**
     * Resets all listbox filter model.
     * @throws InterruptedException
     */
    void onResetFilters() throws InterruptedException;

    /**
     * Resets all models and restores default settings.
     * @throws InterruptedException
     */
    void onResetAll() throws InterruptedException;

    /**
     * Returns list of the enabled normal filters
     * @return normal filters
     */
    List<String> getFilters();

    /**
     * Notifies controller that something was changed.
     */
    void fireChanges();
}