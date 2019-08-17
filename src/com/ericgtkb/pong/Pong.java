package com.ericgtkb.pong;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.media.AudioClip;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.Random;

public class Pong extends Application {
    private static final double BOARD_WIDTH = 1000;
    private static final double BOARD_HEIGHT = 500;
    private static final String helpMessage = "Click \"Start game\" or press space to start!\n" +
                                              "A: left peddle up, Z: left peddle down\n" +
                                              "': right peddle up, /: right peddle down\n" +
                                              "Press ESC to quit";
    private static final int MAX_SCORE = 10;
    private Scene scene;
    private Peddle leftPeddle;
    private Peddle rightPeddle;
    private Ball ball;
    private int leftScore;
    private int rightScore;
    private Label leftLabel;
    private Label rightLabel;
    private boolean leftPeddleUp;
    private boolean leftPeddleDown;
    private boolean rightPeddleUp;
    private boolean rightPeddleDown;
    private AnimationTimer timer;
    private boolean gameActive;
    private Label helpLabel;
    private Label winnerLabel;
    private Button startButton;
    private GraphicsContext graphicsContext;
    private AudioClip collisionSound;
    private AudioClip scoreSound;

    private class Peddle {
        private static final double WIDTH = 30;
        private static final double HEIGHT = 100;
        private static final double DISPLACEMENT = 5;
        private static final double LEFT = 0;
        private static final double RIGHT = 1;
        private double side;
        private double x;
        private double y;
        private double centerX;
        private double centerY;

        public Peddle(double centerX, double centerY, double side) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.side = side;
            x = centerX - WIDTH / 2;
            y = centerY - HEIGHT / 2;
        }

        private void move(double dY) {
            if (centerY - HEIGHT / 2 + dY <= 0) {
                centerY = HEIGHT / 2;
            } else if (centerY + HEIGHT / 2 + dY >= BOARD_HEIGHT) {
                centerY = BOARD_HEIGHT - HEIGHT / 2;
            } else {
                centerY += dY;
            }
            y = centerY - HEIGHT / 2;
        }

    }

    private class Ball {
        private static final double RADIUS = 10;
        private static final double SPEED = 10;
        private static final int RIGHT = 1;
        private static final int LEFT = -1;
        private double x;
        private double y;
        private double centerX;
        private double centerY;
        private double dX;
        private double dY;
        private int direction;

        public Ball() {
            recenter();
            getRandomDirection();
            setMovement();
        }

        private void updateXY() {
            x = centerX - RADIUS;
            y = centerY - RADIUS;
        }

        private void recenter() {
            centerX = BOARD_WIDTH / 2;
            centerY = BOARD_HEIGHT / 2;
            updateXY();
        }

        private void getRandomDirection() {
            Random rand = new Random();
            if (rand.nextBoolean()) {
                direction = RIGHT;
            } else {
                direction = LEFT;
            }
        }

        private void setMovement() {
            Random rand = new Random();
            int upDown;
            if (rand.nextBoolean()) {
                upDown = -1;
            } else {
                upDown = 1;
            }
            dY = upDown * Math.sqrt(SPEED * SPEED / 2) * rand.nextDouble();
            dX = direction * Math.sqrt(SPEED * SPEED - dY * dY);
        }

        private void move() {
            if (centerY - RADIUS + dY <= 0) {
                centerY = RADIUS;
                dY *= -1;
                collisionSound.play();
            } else if (centerY + RADIUS + dY >= BOARD_HEIGHT) {
                centerY = BOARD_HEIGHT - RADIUS;
                dY *= -1;
                collisionSound.play();
            } else {
                centerY += dY;
            }

            centerX += dX;
            updateXY();
        }

        private void checkCollision(Peddle peddle) {
            if (peddle.side == Peddle.LEFT) {
                if (centerY >= peddle.centerY - Peddle.HEIGHT / 2 && centerY <= peddle.centerY + Peddle.HEIGHT / 2
                        && centerX - RADIUS <= peddle.centerX + Peddle.WIDTH / 2) {
                    centerX = peddle.centerX + Peddle.WIDTH / 2 + RADIUS;
                    dX *= -1;
                    collisionSound.play();
                }
            } else {
                if (centerY >= peddle.centerY - Peddle.HEIGHT / 2 && centerY <= peddle.centerY + Peddle.HEIGHT / 2
                        && centerX + RADIUS >= peddle.centerX - Peddle.WIDTH / 2) {
                    centerX = peddle.centerX - Peddle.WIDTH / 2 - RADIUS;
                    dX *= -1;
                    collisionSound.play();
                }
            }
            if (centerY < peddle.centerY && centerX >= peddle.centerX - Peddle.WIDTH / 2
                    && centerX <= peddle.centerX + Peddle.WIDTH / 2
                    && centerY + RADIUS >= peddle.centerY - Peddle.HEIGHT / 2) {
                centerY = peddle.centerY - Peddle.HEIGHT / 2 - RADIUS;
                dY *= -1;
                collisionSound.play();
            } else if (centerY > peddle.centerY && centerX >= peddle.centerX - Peddle.WIDTH / 2
                    && centerX <= peddle.centerX + Peddle.WIDTH / 2
                    && centerY - RADIUS <= peddle.centerY + Peddle.HEIGHT / 2) {
                centerY = peddle.centerY + Peddle.HEIGHT / 2 + RADIUS;
                dY *= -1;
                collisionSound.play();
            }
            updateXY();
        }

        private void checkScore() {
            if (centerX - RADIUS <= 0) {
                scoreSound.play();
                direction = RIGHT;
                updateScoreRight();
                recenter();
                setMovement();
            } else if (centerX + RADIUS >= BOARD_WIDTH) {
                scoreSound.play();
                direction = LEFT;
                updateScoreLeft();
                recenter();
                setMovement();
            }
        }

        private void reset() {
            recenter();
            getRandomDirection();
            setMovement();
        }
    }


    @Override
    public void start(Stage stage) {
        initialize();
        stage.setTitle("Pong game");
        stage.setScene(scene);
        stage.show();
    }

    private void initialize() {
        Pane root = new Pane();
        StackPane background = new StackPane();
        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT);
        graphicsContext = canvas.getGraphicsContext2D();
        canvas.setFocusTraversable(true);
        canvas.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                switch (keyEvent.getCode()) {
                    case A:
                        leftPeddleUp = true;
                        leftPeddleDown = false;
                        break;
                    case Z:
                        leftPeddleDown = true;
                        leftPeddleUp = false;
                        break;
                    case QUOTE:
                        rightPeddleUp = true;
                        rightPeddleDown = false;
                        break;
                    case SLASH:
                        rightPeddleDown = true;
                        rightPeddleUp = false;
                        break;
                    case SPACE:
                        if (!gameActive) {
                            startGame();
                        }
                        break;
                    case ESCAPE:
                        Platform.exit();
                        break;
                }
            }
        });
        canvas.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                switch (keyEvent.getCode()) {
                    case A:
                        leftPeddleUp = false;
                        break;
                    case Z:
                        leftPeddleDown = false;
                        break;
                    case QUOTE:
                        rightPeddleUp = false;
                        break;
                    case SLASH:
                        rightPeddleDown = false;
                        break;
                }
            }
        });

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (leftPeddleUp) {
                    leftPeddle.move(-Peddle.DISPLACEMENT);
                }
                if (leftPeddleDown) {
                    leftPeddle.move(Peddle.DISPLACEMENT);
                }
                if (rightPeddleUp) {
                    rightPeddle.move(-Peddle.DISPLACEMENT);
                }
                if (rightPeddleDown) {
                    rightPeddle.move(Peddle.DISPLACEMENT);
                }
                ball.move();
                ball.checkCollision(leftPeddle);
                ball.checkCollision(rightPeddle);
                draw();
                ball.checkScore();
            }
        };
        leftScore = 0;
        rightScore = 0;
        leftLabel = new Label(Integer.toString(leftScore));
        rightLabel = new Label(Integer.toString(rightScore));
        leftLabel.setTextFill(Color.GREEN);
        leftLabel.setFont(Font.font("Impact", 100));
        rightLabel.setTextFill(Color.BLUE);
        rightLabel.setFont(Font.font("Impact", 100));

        helpLabel = new Label(helpMessage);
        helpLabel.setTextAlignment(TextAlignment.CENTER);
        helpLabel.setTextFill(Color.YELLOW);
        helpLabel.setFont(Font.font("Impact", 30));

        winnerLabel = new Label();
        winnerLabel.setTextAlignment(TextAlignment.CENTER);
        winnerLabel.setTextFill(Color.YELLOW);
        winnerLabel.setFont(Font.font("Impact", 60));
        winnerLabel.setVisible(false);

        startButton = new Button("Start game");
        startButton.setOnAction(actionEvent -> startGame());

        background.setStyle("-fx-background-color: black");
        background.getChildren().addAll(leftLabel, rightLabel, canvas, helpLabel, winnerLabel, startButton);
        StackPane.setAlignment(leftLabel, Pos.TOP_LEFT);
        StackPane.setMargin(leftLabel, new Insets(0, 0, 0, BOARD_WIDTH / 8));
        StackPane.setAlignment(rightLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(rightLabel, new Insets(0, BOARD_WIDTH / 8, 0, 0));
        StackPane.setAlignment(helpLabel, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(winnerLabel, Pos.TOP_CENTER);

        root.getChildren().add(background);
        scene = new Scene(root);

        leftPeddle = new Peddle(Peddle.WIDTH / 2, BOARD_HEIGHT / 2, Peddle.LEFT);
        rightPeddle = new Peddle(BOARD_WIDTH - Peddle.WIDTH / 2, BOARD_HEIGHT / 2, Peddle.RIGHT);

        ball = new Ball();

        draw();

        collisionSound = new AudioClip("file:resources/sounds/collision.aif");
        scoreSound = new AudioClip("file:resources/sounds/score.aif");
    }

    private void draw() {
        graphicsContext.clearRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);

        graphicsContext.setStroke(Color.color(0.25, 0.25, 0.25));
        graphicsContext.setLineWidth(5);
        graphicsContext.setLineDashes(10, 20);
        graphicsContext.strokeLine(BOARD_WIDTH / 2, 0, BOARD_WIDTH / 2, BOARD_HEIGHT);

        graphicsContext.setFill(Color.GREEN);
        graphicsContext.fillRect(leftPeddle.x, leftPeddle.y, Peddle.WIDTH, Peddle.HEIGHT);
        graphicsContext.setFill(Color.BLUE);
        graphicsContext.fillRect(rightPeddle.x, rightPeddle.y, Peddle.WIDTH, Peddle.HEIGHT);

        graphicsContext.setFill(Color.RED);
        graphicsContext.fillOval(ball.x, ball.y, 2 * Ball.RADIUS, 2 * Ball.RADIUS);
    }

    private void startGame() {
        resetScore();
        gameActive = true;
        startButton.setManaged(false);
        startButton.setVisible(false);
        helpLabel.setVisible(false);
        winnerLabel.setVisible(false);
        ball.reset();
        timer.start();
    }

    private void resetScore() {
        leftScore = 0;
        rightScore = 0;
        leftLabel.setText(Integer.toString(leftScore));
        rightLabel.setText(Integer.toString(rightScore));
    }

    private void updateScoreLeft() {
        leftScore += 1;
        leftLabel.setText(Integer.toString(leftScore));
        if (leftScore == MAX_SCORE) {
            endGame("LEFT");
        }
    }

    private void updateScoreRight() {
        rightScore += 1;
        rightLabel.setText(Integer.toString(rightScore));
        if (rightScore == MAX_SCORE) {
            endGame("RIGHT");
        }
    }

    private void endGame(String winner) {
        timer.stop();
        winnerLabel.setText(winner + " WON!!!");
        winnerLabel.setVisible(true);
        gameActive = false;
        startButton.setText("New game");
        startButton.setManaged(true);
        startButton.setVisible(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
