package ru.gb;

import lombok.Data;

import java.io.File;
import java.io.Serializable;

@Data
public class FileObject implements Serializable {

    private final String fileName;
    private final File file;

}
