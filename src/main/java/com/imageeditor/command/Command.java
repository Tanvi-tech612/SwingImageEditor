package com.imageeditor.command;

public interface Command {

    void execute();

    void undo();
}