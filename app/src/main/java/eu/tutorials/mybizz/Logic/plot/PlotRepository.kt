package eu.tutorials.mybizz.Logic.plot

import eu.tutorials.mybizz.Model.Plot

class PlotRepository {
    suspend fun getAllPlots(sheetsRepo: PlotSheetsRepository): List<Plot> {
        return sheetsRepo.getAllPlots()
    }

    suspend fun addPlot(plot: Plot, sheetsRepo: PlotSheetsRepository): Boolean {
        return sheetsRepo.addPlot(plot)
    }

    suspend fun updatePlot(plot: Plot, sheetsRepo: PlotSheetsRepository): Boolean {
        return sheetsRepo.updatePlot(plot)
    }

    suspend fun deletePlot(plotId: String, sheetsRepo: PlotSheetsRepository): Boolean {
        return sheetsRepo.deletePlot(plotId)
    }
}