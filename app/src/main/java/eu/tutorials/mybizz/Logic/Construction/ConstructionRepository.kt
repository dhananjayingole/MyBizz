package eu.tutorials.mybizz.Logic.Construction

import eu.tutorials.mybizz.Model.Construction

class ConstructionRepository {
    suspend fun getAllConstructions(sheetsRepo: ConstructionSheetsRepository): List<Construction> {
        return sheetsRepo.getAllConstructions()
    }

    suspend fun addConstruction(construction: Construction, sheetsRepo: ConstructionSheetsRepository): Boolean {
        return sheetsRepo.addConstruction(construction)
    }

    suspend fun updateConstruction(construction: Construction, sheetsRepo: ConstructionSheetsRepository): Boolean {
        return sheetsRepo.updateConstruction(construction)
    }

    suspend fun deleteConstruction(constructionId: String, sheetsRepo: ConstructionSheetsRepository): Boolean {
        return sheetsRepo.deleteConstruction(constructionId)
    }
}