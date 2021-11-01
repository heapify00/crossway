package it.units.crossway.client.remote;

import it.units.crossway.client.GameHandler;
import it.units.crossway.client.model.PlayerColor;
import it.units.crossway.client.model.StonePlacementIntent;
import lombok.NonNull;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

@Component
public class StompMessageHandler implements StompFrameHandler {

    private final GameHandler gameHandler;

    public StompMessageHandler(GameHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @Override
    @NonNull
    public Type getPayloadType(@NonNull StompHeaders headers) {
        return StonePlacementIntent.class;
    }

    @Override
    public void handleFrame(@NonNull StompHeaders headers, Object payload) {
        if (headers.containsKey("join-event")) {
            System.out.println(headers.getFirst("join-event") + " joined the game");
            gameHandler.startGame();
            return;
        }
        if (headers.containsKey("win-event")) {
            System.out.println("You lose :(\n" + headers.getFirst("win-event") + " win");
            return;
        }
        if (headers.containsKey("pie-rule-event") && gameHandler.getPlayer().getColor().equals(PlayerColor.BLACK)) {
            gameHandler.getPlayer().setColor(PlayerColor.WHITE);
            gameHandler.getTurn().setTurnColor(PlayerColor.BLACK);
        }
        if (payload instanceof StonePlacementIntent) {
            StonePlacementIntent stonePlacementIntent = (StonePlacementIntent) payload;
            gameHandler.getBoard().placeStone(
                    stonePlacementIntent.getRow(),
                    stonePlacementIntent.getColumn(),
                    gameHandler.getTurn().getTurnColor()
            );
        }
        gameHandler.endTurn();
        gameHandler.playTurnIfSupposedTo();
    }
}
