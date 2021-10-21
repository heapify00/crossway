package it.units.crossway.client;

import it.units.crossway.client.exception.PlacementViolationException;
import it.units.crossway.client.model.*;
import lombok.Data;

@Data
public class GameHandler {

    private Player player;
    private Board board;
    private Turn turn;

    public GameHandler() {
        this.player = new Player();
        this.board = new Board();
        this.turn = new Turn();
    }

//    public GameHandler(Player player) {
//        this.player = player;
//        this.board = new Board();
//        this.turn = new Turn();
//    }

    public void startGame() {
        turn.initFirstTurn();
        IOUtils.printMenu();
    }

    public void startGameAtGivenState(Board board) {
        this.player = new Player();
        this.board = board;
        this.turn = new Turn();
    }

    public void startGameAtGivenState(Board board, Turn turn) {
        this.board = board;
        this.turn = turn;
        this.player = new Player();
    }

    public void startGameAtGivenState(Board board, Turn turn, Player player) {
        this.board = board;
        this.turn = turn;
        this.player = player;
    }

    public void playTurn() {
        if (Rules.isPieRuleTurn(turn) && IOUtils.isPieRuleRequested()) {
            turn.applyPieRule();
            return;
        }
        placeStone();
        endTurnChecks();
        turn.nextTurn();
    }

    public void placeStone() {
        StonePlacementIntent stonePlacementIntent = getValidStonePlacementIntent();
        board.placeStone(
                new Intersection(
                        stonePlacementIntent.getRow(),
                        stonePlacementIntent.getColumn()
                ),
                stonePlacementIntent.getPlayer().getColor()
        );
        // todo: send to server
    }

    private StonePlacementIntent getValidStonePlacementIntent() {
        while (true) {
            StonePlacementIntent stonePlacementIntent = getStonePlacementIntent();
            try {
                Rules.validatePlacementIntent(board, stonePlacementIntent);
                return stonePlacementIntent;
            } catch (PlacementViolationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private StonePlacementIntent getStonePlacementIntent() {
        String input = IOUtils.getInputLine();
        int row = getIntRowFromPlayerInput(input);
        int column = getIntColumnFromPlayerInput(input);
        return new StonePlacementIntent(row, column, player);
    }

    private void endTurnChecks() {
        if (Rules.isWinValidTurn(turn) && Rules.checkWin(board, turn.getCurrentPlayer())) {
            endGame();
        }
    }

    private void endGame() {
    }


    private int getIntColumnFromPlayerInput(String input) {
        return Integer.parseInt(input.substring(input.indexOf(",") + 1));
    }

    private int getIntRowFromPlayerInput(String input) {
        return Integer.parseInt(input.substring(0, input.indexOf(",")));
    }

}

