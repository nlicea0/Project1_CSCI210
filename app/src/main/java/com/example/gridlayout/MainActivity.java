package com.example.gridlayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.*;

class CellInfo {
    boolean hasBomb = false;
    boolean hasFlag = false;
    boolean isRevealed = false;
}

public class MainActivity extends AppCompatActivity {

    private static final int COLUMN_COUNT = 10;
    private static final int ROW_COUNT = 12;
    private int[] nearby_bombs = new int[120];
    private int flags_needed = 4;
    private int clock = 0;
    private boolean running = true;
    private boolean endGame = false;
    private boolean endGameBombs = false;
    private int numCellsRevealed = 0;
    private boolean lastClick = false;
    private Set<Integer> bombs = new HashSet<Integer>();

    // save the TextViews of all cells in an array, so later on,
    // when a TextView is clicked, we know which cell it is
    private ArrayList<TextView> cell_tvs;

    private int dpToPixel(int dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // return array of distance to a bomb
    public void distanceToBombs(ArrayList<TextView> tv){
        // for all text views
        for (int i = 0; i < tv.size(); i++){
            // cell has a bomb
            boolean yes_bomb = ((CellInfo) tv.get(i).getTag()).hasBomb;
            if (yes_bomb){
                int row = i / COLUMN_COUNT;
                int col = i % COLUMN_COUNT;

                // Define offsets for the 8 neighboring cells
                int[] rowOffsets = { -1, -1, -1, 0, 0, 1, 1, 1 };
                int[] colOffsets = { -1, 0, 1, -1, 1, -1, 0, 1 };

                for (int j = 0; j < 8; j++) {
                    int newRow = row + rowOffsets[j];
                    int newCol = col + colOffsets[j];

                    // Check if the neighboring cell is within the grid boundaries
                    if (newRow >= 0 && newRow < ROW_COUNT && newCol >= 0 && newCol < COLUMN_COUNT) {
                        int neighborIndex = newRow * COLUMN_COUNT + newCol;
                        nearby_bombs[neighborIndex]++; // Increment the count of the neighboring cell
                    }
                }
            }
        }
    }

    private void runTimer() {
        final TextView timeView = (TextView) findViewById(R.id.textViewTimer);
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                timeView.setText(String.valueOf(clock));

                if (running) {
                    clock++;
                }
                handler.postDelayed(this, 1000);
            }
        });
    }
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("clock", clock);
        savedInstanceState.putBoolean("running", running);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cell_tvs = new ArrayList<TextView>();

        // create timer
        if (savedInstanceState != null) {
            clock = savedInstanceState.getInt("clock");
            running = savedInstanceState.getBoolean("running");
        }
        runTimer();

        // create bombs in graph
        Random rand = new Random();
        while(bombs.size() < 4){
            bombs.add(rand.nextInt(120));
        }

        // Method (2): add four dynamically created cells
        GridLayout grid = (GridLayout) findViewById(R.id.gridLayout01);
        for (int i = 0; i<12; i++) {
            for (int j=0; j<10; j++) {
                TextView tv = new TextView(this);
                tv.setHeight( dpToPixel(34) );
                tv.setWidth( dpToPixel(34) );
                tv.setTextSize( 24 );//dpToPixel(32) );
                tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                tv.setTextColor(Color.GREEN);
                tv.setBackgroundColor(Color.GREEN);
                tv.setOnClickListener(this::onClickTV);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.setMargins(dpToPixel(2), dpToPixel(2), dpToPixel(2), dpToPixel(2));
                lp.rowSpec = GridLayout.spec(i);
                lp.columnSpec = GridLayout.spec(j);

                // cell should be a bomb
                CellInfo cell_info = new CellInfo();
                if(bombs.contains(i * COLUMN_COUNT + j)){
                    cell_info.hasBomb = true;
                }

                tv.setTag(cell_info);
                grid.addView(tv, lp);
                cell_tvs.add(tv);
            }
        }

        // get array of distance to a bomb
        distanceToBombs(cell_tvs);
    }

    private int findIndexOfCellTextView(TextView tv) {
        for (int n=0; n<cell_tvs.size(); n++) {
            if (cell_tvs.get(n) == tv)
                return n;
        }
        return -1;
    }

    // click cell
    public void onClickTV(View view) {
        TextView tv = (TextView) view;
        TextView button_tv = (TextView) findViewById(R.id.textViewButton);
        int n = findIndexOfCellTextView(tv);
        int row = n / COLUMN_COUNT;
        int column = n % COLUMN_COUNT;

        // win game move to result page
        if(endGame){
            // reveal all bombs
            for (Integer bomb : bombs){
                TextView bomb_tv = cell_tvs.get(bomb);
                bomb_tv.setText(R.string.mine);
                bomb_tv.setTextColor(Color.BLACK);
                bomb_tv.setBackgroundColor(Color.LTGRAY);
            }

            if (lastClick){
                String message = "Used " + clock + " seconds.\n" + "You won.\nGood job!";
                Intent intent = new Intent(this, ResultPage.class);
                intent.putExtra("com.example.onClickTV.MESSAGE", message);
                startActivity(intent);
            }

            lastClick = true;
        }
        // lost game move to result page
        else if(endGameBombs){
            String message = "Used " + clock + " seconds.\n" + "You lost.\nBetter luck next time.";
            Intent intent = new Intent(this, ResultPage.class);
            intent.putExtra("com.example.onClickTV.MESSAGE", message);
            startActivity(intent);
        }
        // mine a bomb
        else if((button_tv.getTag()).equals("pick") && tv.getCurrentTextColor() == Color.GREEN && !(((CellInfo) tv.getTag()).hasFlag) && ((CellInfo) tv.getTag()).hasBomb){
            // reveal all bombs
            for (Integer bomb : bombs){
                TextView bomb_tv = cell_tvs.get(bomb);
                bomb_tv.setText(R.string.mine);
                bomb_tv.setTextColor(Color.BLACK);
                bomb_tv.setBackgroundColor(Color.LTGRAY);
            }
            // end the game
            endGameBombs = true;
            running = false;
        }
        // mine a valid cell
        else if ((button_tv.getTag()).equals("pick") && tv.getCurrentTextColor() == Color.GREEN && !(((CellInfo) tv.getTag()).hasFlag)) {
            if(nearby_bombs[n] > 0){
                // set text
                tv.setText(String.valueOf(nearby_bombs[n]));
            }
            // change color
            tv.setTextColor(Color.BLACK);
            tv.setBackgroundColor(Color.LTGRAY);
            numCellsRevealed++;
            if(numCellsRevealed == 116){
                endGame = true;
                running = false;
            }
            // reveal other 8 cells
            revealCell(tv);
        }
        // flag cell
        else if(!(((CellInfo) tv.getTag()).hasFlag) && button_tv.getTag() == "flag" && tv.getCurrentTextColor() == Color.GREEN){
            tv.setText(R.string.flag);

            // update tag
            CellInfo cell_info = new CellInfo();
            cell_info.hasFlag = true;
            tv.setTag(cell_info);

            // update counter
            TextView flagCounter = findViewById(R.id.textViewFlagCount);
            flags_needed--;
            if(flags_needed >= 0){
                flagCounter.setText(String.valueOf(flags_needed));
            }
        }
        // unflag cell
        else if(((CellInfo) tv.getTag()).hasFlag && button_tv.getTag() == "flag"){
            tv.setText("");

            // update tag
            CellInfo cell_info = new CellInfo();
            cell_info.hasFlag = false;
            tv.setTag(cell_info);

            // update counter
            TextView flagCounter = findViewById(R.id.textViewFlagCount);
            flags_needed++;
            if(flags_needed >= 0){
                flagCounter.setText(String.valueOf(flags_needed));
            }
        }
    }

    // reveal neighboring cells
    public void revealCell(TextView tv){
        int n = findIndexOfCellTextView(tv);
        int row = n / COLUMN_COUNT;
        int column = n % COLUMN_COUNT;

        // Define offsets for the 8 neighboring cells
        int[] rowOffsets = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] colOffsets = {-1, 0, 1, -1, 1, -1, 0, 1};

        Vector<TextView> blank_cells = new Vector<TextView>();

        // reveal neighboring cells
        for (int j = 0; j < 8; j++) {
            int newRow = row + rowOffsets[j];
            int newCol = column + colOffsets[j];
            int neighborIndex = newRow * COLUMN_COUNT + newCol;

            // Check if the neighboring cell is within the grid boundaries and unrevealed
            if (newRow >= 0 && newRow < ROW_COUNT && newCol >= 0 && newCol < COLUMN_COUNT
                    && cell_tvs.get(neighborIndex).getCurrentTextColor() == Color.GREEN
                    && !((CellInfo) cell_tvs.get(neighborIndex).getTag()).hasFlag
                    && !((CellInfo) cell_tvs.get(neighborIndex).getTag()).hasBomb) {

                // new cell setup
                TextView neighbor_tv = cell_tvs.get(neighborIndex);
                int n2 = findIndexOfCellTextView(neighbor_tv);

                // set text
                if (((CellInfo) neighbor_tv.getTag()).hasBomb) {
                    neighbor_tv.setText(R.string.mine);
                } else if (nearby_bombs[n2] != 0) {
                    neighbor_tv.setText(String.valueOf(nearby_bombs[n2]));
                }
                else if (nearby_bombs[n2] == 0){
                    blank_cells.add(neighbor_tv);
                }

                // change color to reveal
                neighbor_tv.setTextColor(Color.BLACK);
                neighbor_tv.setBackgroundColor(Color.LTGRAY);
                numCellsRevealed++;
                if(numCellsRevealed == 116){
                    endGame = true;
                    running = false;
                }
            }

            // recursively reveal blank cells
            for (int i = 0; i < blank_cells.size(); i++){
                revealCell(blank_cells.get(i));
            }
        }
    }

    // on click for dig/flag mode
    public void switchMode(View view){
        TextView tv = (TextView) view;
        Object textType = view.getTag();

        // change image
        if(textType.equals("pick")){
            tv.setTag("flag");
            tv.setText(R.string.flag);
        }
        else {
            tv.setTag("pick");
            tv.setText(R.string.pick);
        }
    }
}