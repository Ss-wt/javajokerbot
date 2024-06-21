package com.ss_wt.JavaJokerBot.service;


import com.ss_wt.JavaJokerBot.config.BotConfig;
import com.ss_wt.JavaJokerBot.model.JokeCall;
import com.ss_wt.JavaJokerBot.model.JokeCallRepository;
import com.ss_wt.JavaJokerBot.model.Jokes;
import com.ss_wt.JavaJokerBot.model.JokesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {


    @Autowired
    private JokeCallRepository jokeCallRepository;

    final BotConfig config;

static final String HELP_TEXT = "This bot is created to make jokes. \n\n" +
        "You can execute commands for the main menu on the left or by typing a command: \n\n" +
        "Type /start to see a welcome message \n\n" +
        "Type /help to see this message again \n\n" +
        "Type /jokes to get a list of jokes \n\n" +
        "Type /randomjoke to get a random joke \n\n" ;

    private final JokesRepository jokesRepository;

    public TelegramBot(BotConfig config, JokesRepository jokesRepository) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/jokes", "get a list of jokes"));
        listOfCommands.add(new BotCommand("/randomjoke", "get a random joke"));
        listOfCommands.add(new BotCommand("/top5jokes", "get a top 5 jokes"));

        try{
            this.execute(new SetMyCommands(listOfCommands,new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
        this.jokesRepository = jokesRepository;
        this.jokeCallRepository = jokeCallRepository;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText){
                case "/start":
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                case "/jokes":
                    getAllJokes(chatId, 0, 10);
                    break;
                case "/randomjoke":
                    getRandomJoke(chatId);
                    registerJokeCall(update.getMessage());
                    break;
                case "/top5jokes":
                    getTop5Jokes(chatId);
                    break;
                default:
                    sendMessage(chatId, "Sorry, command was not recognized");
            }
        }
    }


    private void registerJokeCall(Message message) {
        long chatId = message.getChatId();
        String firstName = message.getChat().getFirstName();

        JokeCall jokeCall = new JokeCall();
        jokeCall.setCallTime(LocalDateTime.now());
        jokeCall.setUser_id(message.getChatId());

        try {
            jokeCallRepository.save(jokeCall);
            log.info("Successfully saved joke call for user " + firstName);
        } catch (Exception e) {
            log.error("Error saving joke call for user " + firstName + ": " + e.getMessage());
        }
    }

    private void startCommandReceived(long chatId, String name){

        String answer = "Hi, " + name + " nice to meet you!";
        log.info("Replied to user: " + name);

        sendMessage(chatId, answer);

    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try{
            execute(message);
        }
        catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void getAllJokes(long chatId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Jokes> jokesPage = jokesRepository.findAll(pageable);

        if (jokesPage.isEmpty()) {
            sendMessage(chatId, "No jokes found");
        } else {
            StringBuilder jokesList = new StringBuilder("Jokes:\n");
            for (Jokes joke : jokesPage) {
                jokesList.append(joke.getID()).append(". ").append(joke.getContent()).append("\n");
            }
            sendMessage(chatId, jokesList.toString());
        }
    }


    private void getTop5Jokes(long chatId) {
        List<Jokes> topJokes = jokesRepository.findTop5ByOrderByPopularityDesc();

        if (topJokes.isEmpty()) {
            sendMessage(chatId, "No jokes found");
        } else {
            StringBuilder topJokesList = new StringBuilder("Top 5 Jokes:\n");
            for (Jokes joke : topJokes) {
                topJokesList.append(joke.getID()).append(". ").append(joke.getContent()).append("\n");
            }
            sendMessage(chatId, topJokesList.toString());
        }
    }

    private void getRandomJoke(long chatId) {
        List<Jokes> jokes = (List<Jokes>) jokesRepository.findAll();
        if (jokes.isEmpty()) {
            log.info("No jokes available in the database");
            sendMessage(chatId, "No jokes available at the moment.");
        } else {
            log.info("Total jokes found: " + jokes.size());
            Random random = new Random();
            int index = random.nextInt(jokes.size());
            Jokes joke = jokes.get(index);
            log.info("Select joke ID: " + joke.getID() + ", Joke: " + joke.getContent());
            sendMessage(chatId, joke.getContent());
        }
    }
}
