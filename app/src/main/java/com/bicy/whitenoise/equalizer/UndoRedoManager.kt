package com.bicy.whitenoise.equalizer

import java.util.UUID

sealed class EqCommand {
    abstract fun execute(curve: EqualizerCurve)
    abstract fun undo(curve: EqualizerCurve)
}

class AddPointCommand(private val point: ControlPoint, private val index: Int) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        curve.points.add(index.coerceIn(0, curve.points.size), point)
    }

    override fun undo(curve: EqualizerCurve) {
        curve.points.removeAt(index.coerceIn(0, curve.points.size - 1))
    }
}

class DeletePointCommand(private val point: ControlPoint, private val index: Int) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        if (index in curve.points.indices) curve.points.removeAt(index)
    }

    override fun undo(curve: EqualizerCurve) {
        curve.points.add(index.coerceIn(0, curve.points.size), point)
    }
}

class MovePointCommand(
    private val index: Int,
    private val oldFreq: Float, private val oldGain: Float,
    private val newFreq: Float, private val newGain: Float
) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.let {
            it.frequencyHz = newFreq
            it.gainDb = newGain
        }
    }

    override fun undo(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.let {
            it.frequencyHz = oldFreq
            it.gainDb = oldGain
        }
    }
}

class ChangeTypeCommand(
    private val index: Int,
    private val oldType: EqFilterType,
    private val newType: EqFilterType
) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.filterType = newType
    }

    override fun undo(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.filterType = oldType
    }
}

class ChangeQCommand(
    private val index: Int,
    private val oldQ: Float,
    private val newQ: Float
) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.qOverride = newQ
    }

    override fun undo(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.qOverride = oldQ
    }
}

class ChangeCurveInCommand(
    private val index: Int,
    private val oldMode: CurveInterpolation,
    private val newMode: CurveInterpolation
) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.curveIn = newMode
    }

    override fun undo(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.curveIn = oldMode
    }
}

class ChangeCurveOutCommand(
    private val index: Int,
    private val oldMode: CurveInterpolation,
    private val newMode: CurveInterpolation
) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.curveOut = newMode
    }

    override fun undo(curve: EqualizerCurve) {
        curve.points.getOrNull(index)?.curveOut = oldMode
    }
}

class BatchCommand(private val commands: List<EqCommand>) : EqCommand() {
    override fun execute(curve: EqualizerCurve) {
        commands.forEach { it.execute(curve) }
    }

    override fun undo(curve: EqualizerCurve) {
        commands.reversed().forEach { it.undo(curve) }
    }
}

class UndoRedoManager(private val maxHistory: Int = 20) {
    private val undoStack = ArrayDeque<EqCommand>(maxHistory)
    private val redoStack = ArrayDeque<EqCommand>(maxHistory)

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun execute(command: EqCommand, curve: EqualizerCurve) {
        command.execute(curve)
        undoStack.addLast(command)
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(curve: EqualizerCurve): Boolean {
        if (!canUndo) return false
        val command = undoStack.removeLast()
        command.undo(curve)
        redoStack.addLast(command)
        return true
    }

    fun redo(curve: EqualizerCurve): Boolean {
        if (!canRedo) return false
        val command = redoStack.removeLast()
        command.execute(curve)
        undoStack.addLast(command)
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
