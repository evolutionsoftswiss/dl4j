package ch.evolutionsoft.net.game;

import static ch.evolutionsoft.net.game.NeuralNetConstants.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.indexaccum.IMax;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.evolutionsoft.net.game.tictactoe.TicTacToeConstants;
import ch.evolutionsoft.net.game.tictactoe.TicTacToeGameHelper;
import ch.evolutionsoft.net.game.tictactoe.TicTacToeNeuralDataConverter;

import static ch.evolutionsoft.net.game.tictactoe.TicTacToeConstants.*;

public class NeuralDataHelper {

  public static final String LOG_PLACEHOLDER = "{}";
  public static final String IND_ARRAY_VALUE_SEPARATOR = ":";
  public static final String INPUT = "Example Neural Net Input";
  public static final String LABEL = " Label=";
  private static final char NEW_LINE = '\n';

  private static final Logger logger = LoggerFactory.getLogger(NeuralDataHelper.class);

  private NeuralDataHelper() {
    // Hide constructor
  }

  public static List<Pair<INDArray, INDArray>> printRandomMiniMaxData(
      List<Pair<INDArray, INDArray>> allPlaygroundsResults,
      int numberOfExamples) {

    int numberOfPlaygroundsAndLabels = allPlaygroundsResults.size();
    List<Pair<INDArray, INDArray>> chosenRandomPairs = new LinkedList<>();

    for (int n = 0; n < numberOfExamples && n < numberOfPlaygroundsAndLabels; n++) {

      Pair<INDArray, INDArray> currentRandomPair =
          allPlaygroundsResults.get(randomGenerator.nextInt(numberOfPlaygroundsAndLabels));
      logger.info("MiniMax Input and Output: {}", currentRandomPair);

      chosenRandomPairs.add(currentRandomPair);
    }

    return chosenRandomPairs;
  }

  public static List<Pair<INDArray,INDArray>> printRandomFeedForwardNetInputAndLabels(List<Pair<INDArray, INDArray>> playgroundsAndAdaptedLabels,
      int numberOfExamples) {

    int numberOfInputRows = playgroundsAndAdaptedLabels.size();

    List<Pair<INDArray,INDArray>> chosenExamples = new LinkedList<>();
    for (int n = 0; n < numberOfExamples && n < numberOfInputRows; n++) {

      int randomRow = randomGenerator.nextInt(numberOfInputRows);

      INDArray currentInput = playgroundsAndAdaptedLabels.get(randomRow).getFirst();
      INDArray currentLabel = playgroundsAndAdaptedLabels.get(randomRow).getSecond();
      
      chosenExamples.add(new Pair<>(currentInput, currentLabel));
      
      logger.info(INPUT + LOG_PLACEHOLDER + LABEL + LOG_PLACEHOLDER,
          currentInput,
          currentLabel);
    }
    
    return chosenExamples;
  }

  public static void printRandomConvolutionalNetInputAndLabels(List<Pair<INDArray, INDArray>> adaptedFetauresLabels,
      int numberOfExamples) {

    int numberOfInputsLables = adaptedFetauresLabels.size();

    for (int n = 0; n < numberOfExamples && n < numberOfInputsLables; n++) {

      int randomRow = randomGenerator.nextInt(numberOfInputsLables);

      logger.info(INPUT + NEW_LINE + LOG_PLACEHOLDER + NEW_LINE + LABEL + LOG_PLACEHOLDER,
          adaptedFetauresLabels.get(randomRow).getFirst(),
          adaptedFetauresLabels.get(randomRow).getSecond());
    }
  }

  public static List<Pair<INDArray, INDArray>> readAll(String inputPath, String labelPath) {

    List<Pair<INDArray, INDArray>> allPlaygroundsResult = new ArrayList<>();

    INDArray inputs = readInputs(inputPath);
    INDArray labels = readLabels(labelPath);

    for (int row = 0; row < inputs.shape()[0]; row++) {

      allPlaygroundsResult.add(new Pair<INDArray, INDArray>(inputs.getRow(row), labels.getRow(row)));
    }

    return allPlaygroundsResult;
  }

  public static INDArray readInputs(String inputPath) {

    return Nd4j.readTxtString(NeuralDataHelper.class.getResourceAsStream(inputPath));
  }

  public static INDArray readLabels(String labelPath) {

    return Nd4j.readTxtString(NeuralDataHelper.class.getResourceAsStream(labelPath));
  }
  
  public static void writeLabelDirectories(List<Pair<INDArray, INDArray>> convertedPlaygroundsResults) {
    
    Path directoryPath = Paths.get("input");
    
    for (int label = 0; label < COLUMN_COUNT; label++) {
      
      Path labelDirectoryPath = Paths.get(directoryPath.toString(), String.valueOf(label));
      try {
        Files.createDirectory(labelDirectoryPath);
      } catch (IOException ioe) {
        logger.error("Directory not created.", ioe);
      }
    }
    
    for (int row = 0; row < convertedPlaygroundsResults.size(); row++) {
      
      INDArray currentPlayground = convertedPlaygroundsResults.get(row).getFirst();
      INDArray currentLabel = convertedPlaygroundsResults.get(row).getSecond();
      
      int label = Nd4j.getExecutioner().execAndReturn(new IMax(currentLabel)).getFinalResult().intValue();
      
      Path labeledPlaygroundPath = Paths.get(directoryPath.toString(), String.valueOf(label), String.valueOf(row));
      
      String pl = String.valueOf(currentPlayground).substring(2);
      
      String output = pl.substring(0, pl.length() - 2) + "," + label;
      
      try (PrintWriter writer = new PrintWriter(labeledPlaygroundPath.toString())) {
        
        writer.write(output);
        writer.flush();
      } catch (FileNotFoundException fnfe) {
        logger.error("Output File path not found.", fnfe);
      }
    }
  }

  public static void writeData(List<Pair<INDArray, INDArray>> allPlaygroundsResults) {

    Pair<INDArray, INDArray> stackedPlaygroundsLabels =
        TicTacToeNeuralDataConverter.stackFeedForwardPlaygroundLabels(allPlaygroundsResults);

    Nd4j.writeTxt(stackedPlaygroundsLabels.getFirst(), "inputs.txt");
    Nd4j.writeTxt(stackedPlaygroundsLabels.getSecond(), "labels.txt");

  }

  public static void writeSeparatedData(List<Pair<INDArray, INDArray>> allPlaygroundsResults) {

    List<Pair<INDArray, INDArray>> maxPlaygroundsLabels = new LinkedList<>();
    List<Pair<INDArray, INDArray>> minPlaygroundsLabels = new LinkedList<>();
    
    for (Pair<INDArray, INDArray> current : allPlaygroundsResults) {
      
      if (TicTacToeGameHelper.getCurrentPlayer(current.getFirst()) == TicTacToeConstants.MAX_PLAYER) {
       
        maxPlaygroundsLabels.add(current);
      
      } else {
        
        minPlaygroundsLabels.add(current);
      }
    }

    Pair<INDArray, INDArray> stackedMaxPlaygroundsLabels =
        TicTacToeNeuralDataConverter.stackFeedForwardPlaygroundLabels(maxPlaygroundsLabels);
    Pair<INDArray, INDArray> stackedMinPlaygroundsLabels =
        TicTacToeNeuralDataConverter.stackFeedForwardPlaygroundLabels(minPlaygroundsLabels);
    
    Nd4j.writeTxt(stackedMaxPlaygroundsLabels.getFirst(), "inputsMax.txt");
    Nd4j.writeTxt(stackedMaxPlaygroundsLabels.getSecond(), "labelsMax.txt");
    
    Nd4j.writeTxt(stackedMinPlaygroundsLabels.getFirst(), "inputsMin.txt");
    Nd4j.writeTxt(stackedMinPlaygroundsLabels.getSecond(), "labelsMin.txt");

  }
}
