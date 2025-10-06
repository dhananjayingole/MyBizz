package eu.tutorials.mybizz.Logic.Task

import eu.tutorials.mybizz.Model.Task

class TaskRepository {
    suspend fun getAllTasks(sheetsRepo: TaskSheetsRepository): List<Task> {
        return sheetsRepo.getAllTasks()
    }

    suspend fun addTask(task: Task, sheetsRepo: TaskSheetsRepository): Boolean {
        return sheetsRepo.addTask(task)
    }

    suspend fun updateTask(task: Task, sheetsRepo: TaskSheetsRepository): Boolean {
        return sheetsRepo.updateTask(task)
    }

    suspend fun deleteTask(taskId: String, sheetsRepo: TaskSheetsRepository): Boolean {
        return sheetsRepo.deleteTask(taskId)
    }
}