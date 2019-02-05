package ch.evolutionsoft.net.game.tictactoe;

import static ch.evolutionsoft.net.game.NeuralNetConstants.DOUBLE_COMPARISON_EPSILON;
import static ch.evolutionsoft.net.game.tictactoe.TicTacToeConstants.*;
import static ch.evolutionsoft.net.game.tictactoe.TicTacToeGameHelper.*;

import java.util.LinkedList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

public class TicTacToeNeuralDataConverter {

  public static final double SMALLEST_MAX_WIN = 1;
  public static final double BIGGEST_MIN_WIN = -1;

  private TicTacToeNeuralDataConverter() {
    // Hide constructor
  }

  public static List<Pair<INDArray, INDArray>> generateMultiClassLabelsConvolutional(List<Pair<INDArray, INDArray>> allPlaygroundsResults) {

    List<Pair<INDArray, INDArray>> convertedLabels = convertMultiMiniMaxLabels(allPlaygroundsResults);
    
    List<Pair<INDArray, INDArray>> resultList = new LinkedList<>();

    for (int index = 0; index < allPlaygroundsResults.size(); index++) {

      INDArray playgroundArray = convertedLabels.get(index).getFirst();

      INDArray playgroundImage = TicTacToeNeuralDataConverter.convertTo3x3Image(playgroundArray);
      INDArray playgroundImage4dRow = Nd4j.create(1, IMAGE_CHANNELS, IMAGE_SIZE, IMAGE_SIZE);
      playgroundImage4dRow.putRow(0, playgroundImage);

      resultList.add(new Pair<INDArray, INDArray>(playgroundImage4dRow, convertedLabels.get(index).getSecond()));
    }

    return resultList;
  }

  public static List<Pair<INDArray, INDArray>> convertMiniMaxPlaygroundLabelsToConvolutionalData(
      List<Pair<INDArray, INDArray>> allPlaygroundsResults) {

    List<Pair<INDArray, INDArray>> convertedLabels = convertMiniMaxLabels(allPlaygroundsResults);
    List<Pair<INDArray, INDArray>> resultList = new LinkedList<>();

    for (int index = 0; index < allPlaygroundsResults.size(); index++) {

      INDArray playgroundArray = convertedLabels.get(index).getFirst();

      INDArray playgroundImage = TicTacToeNeuralDataConverter.convertTo3x3Image(playgroundArray);
      INDArray playgroundImage4dRow = Nd4j.create(1, IMAGE_CHANNELS, IMAGE_SIZE, IMAGE_SIZE);
      playgroundImage4dRow.putRow(0, playgroundImage);

      resultList.add(new Pair<INDArray, INDArray>(playgroundImage4dRow, convertedLabels.get(index).getSecond()));
    }

    return resultList;
  }

  public static Pair<INDArray, INDArray> stackFeedForwardPlaygroundLabels(
      List<Pair<INDArray, INDArray>> adaptedPlaygroundsLabels) {

    int playgroundsLabelsSize = adaptedPlaygroundsLabels.size();
    INDArray stackedPlaygrounds = Nd4j.zeros(playgroundsLabelsSize, COLUMN_NUMBER);
    INDArray stackedLabels = Nd4j.zeros(playgroundsLabelsSize, COLUMN_NUMBER);

    for (int index = 0; index < playgroundsLabelsSize; index++) {

      INDArray currentPlayground = adaptedPlaygroundsLabels.get(index).getFirst();
      stackedPlaygrounds.putRow(index, currentPlayground);

      INDArray currentLabel = adaptedPlaygroundsLabels.get(index).getSecond();
      stackedLabels.putRow(index, currentLabel);
    }

    return new Pair<>(stackedPlaygrounds, stackedLabels);
  }

  public static Pair<INDArray, INDArray> stackConvolutionalPlaygroundLabels(
      List<Pair<INDArray, INDArray>> adaptedPlaygroundsLabels) {

    int playgroundsLabelsSize = adaptedPlaygroundsLabels.size();
    INDArray stackedPlaygrounds = Nd4j.zeros(playgroundsLabelsSize, IMAGE_CHANNELS, IMAGE_SIZE, IMAGE_SIZE);
    INDArray stackedLabels = Nd4j.zeros(playgroundsLabelsSize, COLUMN_NUMBER);

    for (int index = 0; index < playgroundsLabelsSize; index++) {

      INDArray currentPlayground = adaptedPlaygroundsLabels.get(index).getFirst();
      stackedPlaygrounds.putRow(index, currentPlayground);

      INDArray currentLabel = adaptedPlaygroundsLabels.get(index).getSecond();
      stackedLabels.putRow(index, currentLabel);
    }

    return new Pair<>(stackedPlaygrounds, stackedLabels);
  }

  public static INDArray convertTo3x3Image(INDArray playgroundArray) {

    INDArray emptyArray = Nd4j.zeros(IMAGE_SIZE, IMAGE_SIZE);
    INDArray maxArray = Nd4j.zeros(IMAGE_SIZE, IMAGE_SIZE);
    INDArray minArray = Nd4j.zeros(IMAGE_SIZE, IMAGE_SIZE);

    for (int row = 0; row < IMAGE_SIZE; row++) {

      for (int column = 0; column < IMAGE_SIZE; column++) {

        int flatIndex = IMAGE_SIZE * row + column;
        double playgroundValue = playgroundArray.getDouble(flatIndex);

        if (playgroundValue == MIN_PLAYER) {

          minArray.putScalar(row, column, 1);

        } else if (playgroundValue == MAX_PLAYER) {

          maxArray.putScalar(row, column, 1);

        } else {

          emptyArray.putScalar(row, column, 1);
        }
      }
    }

    INDArray playgroundImage = Nd4j.create(IMAGE_CHANNELS, IMAGE_SIZE, IMAGE_SIZE);
    playgroundImage.putRow(0, emptyArray);
    playgroundImage.putRow(1, maxArray);
    playgroundImage.putRow(2, minArray);

    return playgroundImage;
  }

  public static List<Pair<INDArray, INDArray>> convertMultiMiniMaxLabels(
      List<Pair<INDArray, INDArray>> allPlaygroundsResults) {

    List<Pair<INDArray, INDArray>> adaptedPlaygroundsLabels = new LinkedList<>();

    for (Pair<INDArray, INDArray> currentPair : allPlaygroundsResults) {
      
      INDArray currentPlayground = currentPair.getFirst();

      INDArray currentResult = currentPair.getSecond();
      INDArray adaptedResult = convertMiniMaxResultToMultiClassNetLabel(currentPlayground, currentResult);

      adaptedPlaygroundsLabels.add(new Pair<>(currentPlayground, adaptedResult));

    }

    return adaptedPlaygroundsLabels;
  }

  public static List<Pair<INDArray, INDArray>> convertMiniMaxLabels(
      List<Pair<INDArray, INDArray>> allPlaygroundsResults) {

    List<Pair<INDArray, INDArray>> adaptedPlaygroundsLabels = new LinkedList<>();

    for (int index = 0; index < allPlaygroundsResults.size(); index++) {

      INDArray currentPlayground = allPlaygroundsResults.get(index).getFirst();

      INDArray currentResult = allPlaygroundsResults.get(index).getSecond();
      INDArray adaptedResult = convertMiniMaxResultToBinaryNetLabel(currentPlayground, currentResult);

      adaptedPlaygroundsLabels.add(new Pair<>(currentPlayground, adaptedResult));
    }

    return adaptedPlaygroundsLabels;
  }

  protected static INDArray convertMiniMaxResultToMultiClassNetLabel(INDArray currentPlayground, INDArray currentResult) {

    int numberOfDrawMoves = 0;
    int numberOfMaxWins = 0;
    int numberOfMinWins = 0;
    for (int arrayIndex = 0; arrayIndex < COLUMN_NUMBER; arrayIndex++) {

      if (equalsEpsilon(currentPlayground.getDouble(0, arrayIndex), EMPTY_FIELD_VALUE,
          DOUBLE_COMPARISON_EPSILON) &&
          equalsEpsilon(currentResult.getDouble(0, arrayIndex), DRAW_VALUE, DOUBLE_COMPARISON_EPSILON)) {

        numberOfDrawMoves++;

      } else if (currentResult.getDouble(0, arrayIndex) >= SMALLEST_MAX_WIN) {

        numberOfMaxWins++;

      } else if (currentResult.getDouble(0, arrayIndex) <= BIGGEST_MIN_WIN) {

        numberOfMinWins++;
      
      }
    }

    INDArray adaptedResult = null;

    if (isMaxMove(currentPlayground) && numberOfMaxWins > 0) {

      adaptedResult = handleMultiMaxWinPosition(currentResult, numberOfMaxWins);

    } else if (!isMaxMove(currentPlayground) && numberOfMinWins > 0) {

      adaptedResult = handleMultiMinWinPosition(currentResult, numberOfMinWins);

    } else if (numberOfDrawMoves > 0) {

      adaptedResult = handleMultiDrawPosition(currentPlayground, currentResult, numberOfDrawMoves);

    } else if (isMaxMove(currentPlayground) && numberOfMinWins > 0) {

      adaptedResult = handleMultiLossPosition(currentPlayground, currentResult, numberOfMinWins);
    
    } else if (!isMaxMove(currentPlayground) && numberOfMaxWins > 0) {

      adaptedResult = handleMultiLossPosition(currentPlayground, currentResult, numberOfMaxWins);
    }

    return adaptedResult;
  }

  protected static INDArray convertMiniMaxResultToBinaryNetLabel(INDArray currentPlayground, INDArray currentResult) {

    int numberOfDrawMoves = 0;
    int numberOfMaxWins = 0;
    int numberOfMinWins = 0;
    for (int arrayIndex = 0; arrayIndex < COLUMN_NUMBER; arrayIndex++) {

      if (equalsEpsilon(currentPlayground.getDouble(0, arrayIndex), EMPTY_FIELD_VALUE,
          DOUBLE_COMPARISON_EPSILON) &&
          equalsEpsilon(currentResult.getDouble(0, arrayIndex), DRAW_VALUE, DOUBLE_COMPARISON_EPSILON)) {

        numberOfDrawMoves++;

      } else if (currentResult.getDouble(0, arrayIndex) >= SMALLEST_MAX_WIN) {

        numberOfMaxWins++;

      } else if (currentResult.getDouble(0, arrayIndex) <= BIGGEST_MIN_WIN) {

        numberOfMinWins++;
      }
    }

    INDArray adaptedResult;

    if (isMaxMove(currentPlayground) && numberOfMaxWins > 0) {

      adaptedResult = handleMaxWinPosition(currentResult, numberOfMaxWins);

    } else if (!isMaxMove(currentPlayground) && numberOfMinWins > 0) {

      adaptedResult = handleMinWinPosition(currentResult, numberOfMinWins);

    } else if (numberOfDrawMoves > 0) {

      adaptedResult = handleDrawPosition(currentPlayground, currentResult, numberOfDrawMoves);

    } else {
      
      adaptedResult = handleLossPosition(currentPlayground);
    }

    return adaptedResult;
  }

  protected static INDArray handleMultiMaxWinPosition(INDArray currentResult, int maxWins) {

    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);
    double maxValue = 1.0;

    int winFieldsFound = 0;
    double fastestWinFieldValue = SMALLEST_MAX_WIN - DEPTH_ADVANTAGE;
    for (int arrayIndex = 0; arrayIndex < 9 && winFieldsFound < maxWins; arrayIndex++) {

      double currentWinFieldValue = currentResult.getDouble(0, arrayIndex);

      if (currentWinFieldValue > fastestWinFieldValue) {

        fastestWinFieldValue = currentWinFieldValue;

        adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);
        adaptedResult.putScalar(0, arrayIndex, maxValue);
        winFieldsFound++;

      } else if (currentWinFieldValue > DRAW_VALUE && equalsEpsilon(currentWinFieldValue, fastestWinFieldValue, DOUBLE_COMPARISON_EPSILON) ) {

        adaptedResult.putScalar(0, arrayIndex, maxValue);
        winFieldsFound++;
      }
    }
    return adaptedResult;
  }

  protected static INDArray handleMaxWinPosition(INDArray currentResult, int maxWins) {

    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);
    double maxValue = 1.0;

    int winFieldsFound = 0;
    double fastestWinFieldValue = SMALLEST_MAX_WIN - DEPTH_ADVANTAGE;
    for (int arrayIndex = 0; arrayIndex < 9 && winFieldsFound < maxWins; arrayIndex++) {

      double currentWinFieldValue = currentResult.getDouble(0, arrayIndex);

      if (currentWinFieldValue > fastestWinFieldValue) {

        fastestWinFieldValue = currentWinFieldValue;

        adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);
        adaptedResult.putScalar(0, arrayIndex, maxValue);

        winFieldsFound++;

      }
    }
    return adaptedResult;
  }

  protected static INDArray handleMultiMinWinPosition(INDArray currentResult, int minWins) {

    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);
    double minValue = 1.0;

    int winFieldsFound = 0;
    double fastestWinFieldValue = BIGGEST_MIN_WIN + DEPTH_ADVANTAGE;
    for (int arrayIndex = 0; arrayIndex < 9 && winFieldsFound < minWins; arrayIndex++) {

      double currentWinFieldValue = currentResult.getDouble(0, arrayIndex);

      if (currentWinFieldValue < fastestWinFieldValue) {

        adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);

        fastestWinFieldValue = currentWinFieldValue;
        adaptedResult.putScalar(0, arrayIndex, minValue);

        winFieldsFound++;

      } else if (currentWinFieldValue < DRAW_VALUE && equalsEpsilon(currentWinFieldValue, fastestWinFieldValue, DOUBLE_COMPARISON_EPSILON) ) {

        adaptedResult.putScalar(0, arrayIndex, minValue);
        winFieldsFound++;
      }
    }
    return adaptedResult;
  }

  protected static INDArray handleMinWinPosition(INDArray currentResult, int minWins) {

    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);
    double minValue = 1.0;

    int winFieldsFound = 0;
    double fastestWinFieldValue = BIGGEST_MIN_WIN + DEPTH_ADVANTAGE;
    for (int arrayIndex = 0; arrayIndex < 9 && winFieldsFound < minWins; arrayIndex++) {

      double currentWinFieldValue = currentResult.getDouble(0, arrayIndex);

      if (currentWinFieldValue < fastestWinFieldValue) {

        adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);

        fastestWinFieldValue = currentWinFieldValue;
        adaptedResult.putScalar(0, arrayIndex, minValue);

        winFieldsFound++;
      }
    }
    return adaptedResult;
  }

  protected static INDArray handleDrawPosition(INDArray currentPlayground, INDArray currentResult, int draws) {

    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);

    double drawValue = 1.0;
    int drawFieldsFound = 0;

    for (int arrayIndex = 0; arrayIndex < 9 && drawFieldsFound < draws; arrayIndex++) {

      if (equalsEpsilon(currentPlayground.getDouble(0, arrayIndex), EMPTY_FIELD_VALUE,
          DOUBLE_COMPARISON_EPSILON) &&
          equalsEpsilon(currentResult.getDouble(0, arrayIndex), DRAW_VALUE, DOUBLE_COMPARISON_EPSILON)) {

        if (drawFieldsFound == draws - 1) {
        
          adaptedResult.putScalar(0, arrayIndex, drawValue);
        }
        drawFieldsFound++;
      }
    }

    return adaptedResult;
  }

  protected static INDArray handleMultiDrawPosition(INDArray currentPlayground, INDArray currentResult, int draws) {

    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);

    double drawValue = 1.0;

    for (int arrayIndex = 0; arrayIndex < 9; arrayIndex++) {

      if (equalsEpsilon(currentPlayground.getDouble(0, arrayIndex), EMPTY_FIELD_VALUE,
          DOUBLE_COMPARISON_EPSILON) &&
          equalsEpsilon(currentResult.getDouble(0, arrayIndex), DRAW_VALUE, DOUBLE_COMPARISON_EPSILON)) {

        adaptedResult.putScalar(0, arrayIndex, drawValue);
      }
    }

    return adaptedResult;
  }

  protected static INDArray handleLossPosition(INDArray currentPlayground) {

    double lossValue = 1.0;
    // Take the last found empty field leading to loss
    boolean lossFieldFound = false;
    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);

    for (int arrayIndex = 0; arrayIndex < 9 && !lossFieldFound; arrayIndex++) {

      if (equalsEpsilon(currentPlayground.getDouble(0, arrayIndex), EMPTY_FIELD_VALUE,
          DOUBLE_COMPARISON_EPSILON)) {

        lossFieldFound = true;
        adaptedResult.putScalar(0, arrayIndex, lossValue);
      }
    }

    return adaptedResult;
  }

  protected static INDArray handleMultiLossPosition(INDArray currentPlayground, INDArray currentResult, int losses) {

    double lossValue = 1.0;

    INDArray adaptedResult = Nd4j.zeros(ROW_NUMBER, COLUMN_NUMBER);

    for (int arrayIndex = 0; arrayIndex < 9; arrayIndex++) {

      if (equalsEpsilon(currentPlayground.getDouble(0, arrayIndex), EMPTY_FIELD_VALUE,
          DOUBLE_COMPARISON_EPSILON)) {

        adaptedResult.putScalar(0, arrayIndex, lossValue);
      }
    }

    return adaptedResult;
  }

}
