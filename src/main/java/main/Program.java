package main;

import Extractors.PdfExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class Program {

    public static void main(String[] args) {
        Path inputsPath = FileSystems.getDefault().getPath("inputs");
        try{
            List<File> files = Files.list(inputsPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .map(Path::toFile)
                    .toList();
            for (File file:files
                 ) {
                PdfExtractor extractor = new PdfExtractor(file);
                extractor.extract();
            }
        }catch (IOException e){
            System.out.println(e);
        }
    }
}
