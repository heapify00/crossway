package it.units.crossway.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import it.units.crossway.client.model.Board;
import it.units.crossway.client.model.Player;
import it.units.crossway.client.model.PlayerColor;
import it.units.crossway.client.model.Turn;
import it.units.crossway.client.model.dto.GameCreationIntent;
import it.units.crossway.client.model.dto.GameDto;
import it.units.crossway.client.model.dto.PlayerDto;
import it.units.crossway.client.remote.Api;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameHandlerTests {

    @Mock
    private Player player;
    @Mock
    private Api api;
    private static WireMockServer wireMockServer;

    @BeforeAll
    static void initWireMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().port(9111));
        wireMockServer.start();
    }

    @AfterAll
    static void shutdownWireMockServer() {
        wireMockServer.shutdown();
    }

    Api buildAndReturnFeignClient() {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .contract(new SpringMvcContract())
                .target(Api.class, "http://localhost:9111");
    }

    @Test
    void whenInitGameShouldInitializeTurn() {
        Board board = new Board();
        Turn turn = new Turn();
        GameHandler gameHandler = new GameHandler(player, board, turn, api);
        gameHandler.initGame();
        assertEquals(gameHandler.getTurn(), new Turn(1, PlayerColor.BLACK));
    }

    @Test
    void whenChooseNicknameShouldSendAddPlayerReq() throws JsonProcessingException {
        Api api = buildAndReturnFeignClient();
        String nickname = "playerXZX";
        Player player = new Player(nickname, null);
        Board board = new Board();
        Turn turn = new Turn();
        GameHandler gameHandler = new GameHandler(player, board, turn, api);
        PlayerDto playerDto = new PlayerDto(nickname);
        ObjectMapper om = new ObjectMapper();
        String jsonPlayerDto = om.writeValueAsString(playerDto);
        wireMockServer.stubFor(post(urlEqualTo("/players"))
                .withRequestBody(equalToJson(jsonPlayerDto))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(jsonPlayerDto)));
        String input = nickname + System.lineSeparator();
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        IOUtils.scanner = new Scanner(System.in);
        gameHandler.chooseNickname();
        assertEquals(nickname, gameHandler.getPlayer().getNickname());
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/players")));
    }

    @Test
    void whenPlayerSelectsNewGameShouldSendCreateGameReq() throws JsonProcessingException {
        Api api = buildAndReturnFeignClient();
        String nickname = "playerXZX";
        Player player = new Player(nickname, null);
        Board board = new Board();
        Turn turn = new Turn();
        GameHandler gameHandler = new GameHandler(player, board, turn, api);
        gameHandler.getPlayer().setNickname(nickname);
        GameCreationIntent gameCreationIntent = new GameCreationIntent(nickname);
        ObjectMapper om = new ObjectMapper();
        String jsonGameCreationIntent = om.writeValueAsString(gameCreationIntent);
        wireMockServer.stubFor(post(urlEqualTo("/games"))
                .withRequestBody(equalToJson(jsonGameCreationIntent))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(jsonGameCreationIntent)));
        String createGameIntent = "1" + System.getProperty("line.separator");
        IOUtils.scanner = new Scanner(new ByteArrayInputStream(createGameIntent.getBytes()));
        gameHandler.chooseGameType();
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/games")));
    }

    @Test
    void whenPlayerSelectsJoinGameShouldSendGetAvailableGamesReq() throws JsonProcessingException {
        Api api = buildAndReturnFeignClient();
        Player player = new Player();
        Board board = new Board();
        Turn turn = new Turn();
        GameHandler gameHandler = new GameHandler(player, board, turn, api);
        ObjectMapper om = new ObjectMapper();
        List<GameDto> availableGames = new ArrayList<>();
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        GameDto gameDto1 = new GameDto(uuid1, null, "blackP1");
        GameDto gameDto2 = new GameDto(uuid2, null, "blackP2");
        availableGames.add(gameDto1);
        availableGames.add(gameDto2);
        String jsonAvailableGames = om.writeValueAsString(availableGames);
        wireMockServer.stubFor(get(urlEqualTo("/games"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(jsonAvailableGames)));
        wireMockServer.stubFor(put(urlEqualTo("/games/" + uuid1))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(om.writeValueAsString(gameDto1))));
        String input = "2" + System.lineSeparator() + "0" + System.lineSeparator();
        IOUtils.redirectScannerToSimulatedInput(input);
        gameHandler.chooseGameType();
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/games")));
    }

    @Test
    void whenPlayerSelectsJoinGameShouldListAvailableGames() throws JsonProcessingException {
        Api api = buildAndReturnFeignClient();
        Player player = new Player();
        Board board = new Board();
        Turn turn = new Turn();
        GameHandler gameHandler = new GameHandler(player, board, turn, api);
        ObjectMapper om = new ObjectMapper();
        List<GameDto> availableGames = new ArrayList<>();
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        GameDto gameDto1 = new GameDto(uuid1, null, "blackP1");
        GameDto gameDto2 = new GameDto(uuid2, null, "blackP2");
        availableGames.add(gameDto1);
        availableGames.add(gameDto2);
        String jsonAvailableGames = om.writeValueAsString(availableGames);
        wireMockServer.stubFor(get(urlEqualTo("/games"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(jsonAvailableGames)));
        wireMockServer.stubFor(put(urlEqualTo("/games/" + uuid1))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(om.writeValueAsString(gameDto1))));
        String input = "2" + System.lineSeparator() + "0" + System.lineSeparator();
        IOUtils.redirectScannerToSimulatedInput(input);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(byteArrayOutputStream));
        gameHandler.chooseGameType();
        assertTrue(byteArrayOutputStream.toString().contains("opponent is blackP1"));
        assertTrue(byteArrayOutputStream.toString().contains("opponent is blackP2"));
    }

    @Test
    void whenPlayerSelectsAnAvailableGameShouldSendJoinGameReq() throws JsonProcessingException {
        Api api = buildAndReturnFeignClient();
        Player player = new Player();
        Board board = new Board();
        Turn turn = new Turn();
        GameHandler gameHandler = new GameHandler(player, board, turn, api);
        ObjectMapper om = new ObjectMapper();
        List<GameDto> availableGames = new ArrayList<>();
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        GameDto gameDto1 = new GameDto(uuid1, null, "blackP1");
        GameDto gameDto2 = new GameDto(uuid2, null, "blackP2");
        availableGames.add(gameDto1);
        availableGames.add(gameDto2);
        String jsonAvailableGames = om.writeValueAsString(availableGames);
        wireMockServer.stubFor(get(urlEqualTo("/games"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(jsonAvailableGames)));
        wireMockServer.stubFor(put(urlEqualTo("/games/" + uuid1))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(om.writeValueAsString(gameDto1))));
        String input = "2" + System.lineSeparator() + "0" + System.lineSeparator();
        IOUtils.redirectScannerToSimulatedInput(input);
        gameHandler.chooseGameType();
        wireMockServer.verify(1, putRequestedFor(urlEqualTo("/games/" + uuid1)));
    }

    @Test
    void whenPlayerJoinsAnAvailableGameShouldSetUuidAndWhitePlayerColor() throws JsonProcessingException {
        Api api = buildAndReturnFeignClient();
        Player player = new Player();
        Board board = new Board();
        Turn turn = new Turn();
        GameHandler gameHandler = new GameHandler(player, board, turn, api);
        ObjectMapper om = new ObjectMapper();
        List<GameDto> availableGames = new ArrayList<>();
        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        GameDto gameDto1 = new GameDto(uuid1, null, "blackP1");
        GameDto gameDto2 = new GameDto(uuid2, null, "blackP2");
        availableGames.add(gameDto1);
        availableGames.add(gameDto2);
        String jsonAvailableGames = om.writeValueAsString(availableGames);
        wireMockServer.stubFor(get(urlEqualTo("/games"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(jsonAvailableGames)));
        wireMockServer.stubFor(put(urlEqualTo("/games/" + uuid1))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(om.writeValueAsString(gameDto1))));
        String input = "2" + System.lineSeparator() + "0" + System.lineSeparator();
        IOUtils.redirectScannerToSimulatedInput(input);
        gameHandler.chooseGameType();
        assertEquals(uuid1, gameHandler.getUuid());
        assertEquals(PlayerColor.WHITE, gameHandler.getPlayer().getColor());
    }


//
//    @Test
//    void whenCreateGameAndGameStartsThenPlayerShouldBeBlack() {
//        // TODO
//        fail();
//    }
//
//    @Test
//    void whenJoinGameAndGameStartsThenPlayerShouldBeWhite() {
//        // TODO
//        fail();
//    }
//
//    @Test
//    void whenPlayerPlaysTurnShouldSendStonePlacementIntent() {
//        // TODO
//        fail();
//    }
//
//    @Test
//    void whenStonePlacementIntentIsReceivedThenBoardShouldBeUpdated() {
//        // TODO
//        fail();
//    }

//    @Test
//    void whenGameStartsBoardShouldBeEmpty() {
//        Turn turn = new Turn();
//        Board board = new Board();
//        gameHandler = new GameHandler(player, board, turn, api);
//        gameHandler.getTurn().initFirstTurn();
//        assertTrue(gameHandler.getBoard().getBoardState().isEmpty());
//    }
//
//    @Test
//    void whenGameStartsTurnShouldBeBlack() {
//        Turn turn = new Turn();
//        Board board = new Board();
//        gameHandler = new GameHandler(player, board, turn, api);
//        gameHandler.getTurn().initFirstTurn();
//        assertEquals(PlayerColor.BLACK, gameHandler.getTurn().getCurrentPlayer());
//    }

//    @Test
//    void whenPlayerSelectsNewGameShouldSendCreateGameReq() throws JsonProcessingException {
//        initWireMockServer();
//        Api api = buildAndReturnFeignClient();
//        String nickname = "playerXZX";
//        Player player = new Player(nickname, null);
//        Board board = new Board();
//        Turn turn = new Turn();
//        GameHandler gameHandler = new GameHandler(player, board, turn, api);
//        gameHandler.getPlayer().setNickname(nickname);
//        GameCreationIntent gameCreationIntent = new GameCreationIntent(nickname);
//        ObjectMapper om = new ObjectMapper();
//        String jsonGameCreationIntent = om.writeValueAsString(gameCreationIntent);
//        wireMockServer.stubFor(post(urlEqualTo("/games"))
//                .withRequestBody(equalToJson(jsonGameCreationIntent))
//                .willReturn(aResponse()
//                        .withStatus(200)
//                        .withBody(jsonGameCreationIntent)));
//        String createGameIntent = nickname + System.getProperty("line.separator") + "1" + System.getProperty("line.separator");
//        System.setIn(new ByteArrayInputStream(createGameIntent.getBytes()));
//        ReflectionTestUtils.invokeMethod(gameHandler, "createNewGame");
//        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/games")));
//    }


}
