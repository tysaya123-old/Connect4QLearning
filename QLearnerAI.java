import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.io.*;


public class QLearnerAI extends AIModule{

    private static double gamma = 0.99;
    public static HashMap<String, int[]> state_action_count = new HashMap<>();
    public static HashMap<String, String[]> state_action_values = new HashMap<>();
    int is_training;

    int k = 1;
    static final double LOSS_REWARD = -1;
    static final double WIN_REWARD = +1;
    static final double TIE_REWARD = 0;

    Random r = new Random();

    public QLearnerAI(int is_training){
        this.is_training = is_training;
        seed();
    }

    class Board{
        String state;
        ArrayList<Integer> legalActions;
        String[] q_values;
        public Board(ArrayList<Integer> legalActions, String state, String[] q_values){
            this.legalActions = legalActions;
            this.state = state;
            this.q_values = q_values;
        }

    }

    private void seed(){

    }


    @Override
    public void getNextMove(GameStateModule game) {
        if (is_training == 1){
            Board curr_board = getStateActionValues(game);
            chosenMove = selectMove(curr_board.legalActions, curr_board.q_values, true);
            updateQTable(game, curr_board);
        }else{
            try{
                Board curr_board = getStateActionValuesFromFile(game);
                chosenMove = selectMove(curr_board.legalActions, curr_board.q_values, false);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    private Board getStateActionValuesFromFile(GameStateModule game) throws Exception{
        String currState = "";
        int nonzeros = 0;
        ArrayList<Integer> legalActions = new ArrayList<>();
        for(int i=0;i<game.getWidth();i++) {
            // legal actions
            if (game.canMakeMove(i)) {
                legalActions.add(i);
            }
            // current state
            for (int j = 0; j < game.getHeight(); j++) {
                currState += String.valueOf(game.getAt(i, j));
                if (game.getAt(i, j) != 0)
                    nonzeros += 1;
            }
        }
        File f = new File("qtables/" + nonzeros + ".txt");
        BufferedReader br = new BufferedReader(new FileReader(f));
        String[] q_values = new String[game.getWidth()];
        for (int i=0;i<q_values.length;i++){
            q_values[i] = "0";
        }
        String line;
        while ((line = br.readLine())!=null){
            line = line.substring(0, line.length()-1);
            String[] spl = line.split(":");
            if (spl[0].equals(currState)) {
                q_values = spl[1].split(" ");
                break;
            }
        }
        return new Board(legalActions, currState, q_values);
    }


    private Board getStateActionValues(GameStateModule game){
        String currState = "";
        ArrayList<Integer> legalActions = new ArrayList<>();
        for(int i=0;i<game.getWidth();i++){

            if (game.canMakeMove(i)){
                legalActions.add(i);
            }

            for(int j=0;j<game.getHeight();j++){
                currState += String.valueOf(game.getAt(i, j));
            }
        }

        String[] q_values = state_action_values.get(currState);
        if (q_values == null){
            String[] action_values = new String[game.getWidth()];
            for (int i=0;i<game.getWidth();i++)
                action_values[i] = "0";
            q_values = action_values;
            state_action_values.put(currState, q_values);
            state_action_count.put(currState, new int[game.getWidth()]);
        }
        return new Board(legalActions, currState, q_values);

    }


    private int selectMove(ArrayList<Integer> legalActions, String[] q_values, Boolean training){
        if(training){
            Double prob = r.nextDouble();
            Double denom = 0.0;
            for(int i = 0; i < legalActions.size(); i++){
                denom += Math.pow(k, getQi(q_values, legalActions.get(i)));
            }
            Double numer = 0.0;
            for(int i = 0; i < legalActions.size(); i++){
                numer += Math.pow(k, getQi(q_values, legalActions.get(i)));
                if(prob <= numer/denom) return legalActions.get(i);
            }


            //for use only if cant do explore vs exploit Or just in case probability fucks up.
            return legalActions.get(r.nextInt(legalActions.size()));
        }
        else{
            int currMove = legalActions.get(r.nextInt(legalActions.size()));
            Double currBest = getQi(q_values, currMove);
            //System.out.println("=========================");
            for(int i = 0; i < legalActions.size(); i++){
                //System.out.println("Col: " + legalActions.get(i) + " is " + getQi(q_values, legalActions.get(i)));
                if(getQi(q_values, legalActions.get(i)) > currBest){
                    currBest = getQi(q_values, legalActions.get(i));
                    currMove = legalActions.get(i);
                }
            }
            //System.out.println("=========================");
            return currMove;
        }
    }

    private void updateQTable(GameStateModule game, Board curr_board){

        String state = curr_board.state;
        int[] counts = state_action_count.get(state);
        Double count = Double.valueOf(counts[chosenMove]);

        Double reward = getReward(game, chosenMove);
        Double qf = getQf(game, chosenMove);
        Double qi = getQi(curr_board.q_values, chosenMove);

        Double alpha = 1/(1+count);

        curr_board.q_values[chosenMove] = Double.toString((1 - alpha)*qi + alpha*(reward + gamma*qf));
        state_action_values.put(state, curr_board.q_values);


        counts[chosenMove]++;
        state_action_count.put(state, counts);
        // update q(s, a) and count(s, a)
    }

    private Double getQi(String[] qs, int move){
        return Double.parseDouble(qs[move]);
    }

    private Double getQf(GameStateModule game, int yourMove) {
        GameStateModule copyGame = game.copy();

        copyGame.makeMove(yourMove);
        if(copyGame.isGameOver()) return 0.0;

        Board theirBoard = getStateActionValues(copyGame);
        int theirMove = selectMove(theirBoard.legalActions, theirBoard.q_values, false);

        copyGame.makeMove(theirMove);
        if(copyGame.isGameOver()) return 0.0;
        else {
            Board nextBoard = getStateActionValues(copyGame);
            Double qf = LOSS_REWARD;
            for (int i = 0; i < nextBoard.q_values.length; i++) {
                Double curr_q = getQi(nextBoard.q_values, i);
                if (curr_q > qf) qf = curr_q;
            }
            return qf;
        }
    }

    private Double getReward(GameStateModule game, int yourMove) {
        GameStateModule copyGame = game.copy();
        copyGame.makeMove(yourMove);
        if(copyGame.isGameOver()){
            return WIN_REWARD;
        }
        else {
            Board theirBoard = getStateActionValues(copyGame);
            int theirMove = selectMove(theirBoard.legalActions, theirBoard.q_values, false);
            copyGame.makeMove(theirMove);
            if (copyGame.isGameOver()) {
                if(copyGame.getWinner() == 0){
                    return TIE_REWARD;
                }
                else{
                    return LOSS_REWARD;
                }
            }
            else{
                //TODO: place eval reward func here.(use copy game)
                return 0.0;
            }
        }
    }


}
