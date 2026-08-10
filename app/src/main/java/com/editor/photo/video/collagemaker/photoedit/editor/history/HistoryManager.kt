package com.editor.photo.video.collagemaker.photoedit.editor.history

import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation

class HistoryManager(private val maxStackSize: Int = 50) {
    private val undoStack = ArrayDeque<List<EditOperation>>()
    private val redoStack = ArrayDeque<List<EditOperation>>()

    private var currentOperations = mutableListOf<EditOperation>()

    fun getActiveOperations(): List<EditOperation> = currentOperations.toList()

    fun addOperation(operation: EditOperation) {
        undoStack.addLast(currentOperations.toList())
        if (undoStack.size > maxStackSize) {
            undoStack.removeFirst()
        }
        currentOperations.add(operation)
        redoStack.clear()
    }

    fun setOperations(operations: List<EditOperation>) {
        undoStack.addLast(currentOperations.toList())
        if (undoStack.size > maxStackSize) {
            undoStack.removeFirst()
        }
        currentOperations = operations.toMutableList()
        redoStack.clear()
    }

    fun undo(): Boolean {
        if (!canUndo()) return false
        val previousState = undoStack.removeLast()
        redoStack.addLast(currentOperations.toList())
        currentOperations = previousState.toMutableList()
        return true
    }

    fun redo(): Boolean {
        if (!canRedo()) return false
        val nextState = redoStack.removeLast()
        undoStack.addLast(currentOperations.toList())
        currentOperations = nextState.toMutableList()
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        currentOperations.clear()
    }
}
