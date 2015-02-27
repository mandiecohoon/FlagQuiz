package com.mandiecohoon.flagtest;


import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class QuizFragment extends Fragment {
   private static final String TAG = "FlagQuiz Activity";

   private static final int FLAGS_IN_QUIZ = 10; 
   
   private List<String> fileNameList;
   private List<String> quizCountriesList;
   private Set<String> regionsSet;
   private String correctAnswer;
   private int totalGuesses; 
   private int correctAnswers;
   private int guessRows; 
   private SecureRandom random;
   private Handler handler;
   private Animation shakeAnimation;
   //private int[] highscores = new int[6];
   
   private TextView questionNumberTextView;
   private ImageView flagImageView;
   private LinearLayout[] guessLinearLayouts;
   private TextView answerTextView;
   
   private boolean firstTry = true;
   private int firstTryCount;
   private int guessScore = 0;
   private int player1Score = 0;
   private int player2Score = 0;
   private int player = 1;
   private boolean newGame;
   private String otherOption1;
   private String otherOption2;
   
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);    
      View view = inflater.inflate(R.layout.fragment_quiz, container, false);

      fileNameList = new ArrayList<String>();
      quizCountriesList = new ArrayList<String>();
      random = new SecureRandom(); 
      handler = new Handler(); 
      player = 1;
      //newGame = true;
      shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake); 
      shakeAnimation.setRepeatCount(3);

      questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
      flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
      guessLinearLayouts = new LinearLayout[3];
      guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
      guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
      guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
      answerTextView = (TextView) view.findViewById(R.id.answerTextView);

      for (LinearLayout row : guessLinearLayouts) {
         for (int column = 0; column < row.getChildCount(); column++) {
            Button button = (Button) row.getChildAt(column);
            button.setOnClickListener(guessButtonListener);
         }
      }  
    
      questionNumberTextView.setText(getResources().getString(R.string.question, 1, FLAGS_IN_QUIZ));

      return view;
   }
   
   public void updateGuessRows(SharedPreferences sharedPreferences) {
      String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
      guessRows = Integer.parseInt(choices) / 3;

      for (LinearLayout layout : guessLinearLayouts)
         layout.setVisibility(View.INVISIBLE);

      for (int row = 0; row < guessRows; row++) 
         guessLinearLayouts[row].setVisibility(View.VISIBLE);
   }
   
   public void updateRegions(SharedPreferences sharedPreferences) {
      regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
   }  

   public void resetQuiz() {
      AssetManager assets = getActivity().getAssets(); 
      fileNameList.clear();
      guessScore = 0;
      firstTry = true;
      firstTryCount = 0;
      
      if (newGame) {
    	  player = 1;
      } else {
    	  player = 2;
      }
      
      try {
         for (String region : regionsSet) {
            String[] paths = assets.list(region);

            for (String path : paths) {
            	fileNameList.add(path.replace(".png", ""));
            }
         } 
      } catch (IOException exception) {
         Log.e(TAG, "Error loading image file names", exception);
      } 
      
      correctAnswers = 0;
      totalGuesses = 0;
      quizCountriesList.clear();
      
      int flagCounter = 1; 
      int numberOfFlags = fileNameList.size(); 

      while (flagCounter <= FLAGS_IN_QUIZ) {
         int randomIndex = random.nextInt(numberOfFlags); 

         String fileName = fileNameList.get(randomIndex);
         
         if (!quizCountriesList.contains(fileName)) {
            quizCountriesList.add(fileName);
            ++flagCounter;
         } 
      } 

      loadNextFlag();
   }

     private void loadNextFlag() {
      String nextImage = quizCountriesList.remove(0);
      correctAnswer = nextImage;
      answerTextView.setText("");
      firstTry = true;
      
      questionNumberTextView.setText(getResources().getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

      String region = nextImage.substring(0, nextImage.indexOf('-'));
      
      AssetManager assets = getActivity().getAssets(); 

      try {
         InputStream stream = assets.open(region + "/" + nextImage + ".png");
         
         Drawable flag = Drawable.createFromStream(stream, nextImage);
         flagImageView.setImageDrawable(flag);                       
      } catch (IOException exception) {
         Log.e(TAG, "Error loading " + nextImage, exception);
      } 

      Collections.shuffle(fileNameList);

      int correct = fileNameList.indexOf(correctAnswer);
      fileNameList.add(fileNameList.remove(correct));
      otherOption1 = "";
      otherOption2 = "";
      
      for (int row = 0; row < guessRows; row++) {
         for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++) { 
            Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
            newGuessButton.setEnabled(true);

            String fileName = fileNameList.get((row * 3) + column);
            newGuessButton.setText(getCountryName(fileName));
            if (otherOption1 == null || otherOption1 == "") {
            	otherOption1 = getCapitalName(fileName);
            } else {
            	otherOption2 = getCapitalName(fileName);
            }
         } 
      } 
      
      int row = random.nextInt(guessRows);
      int column = random.nextInt(3);
      LinearLayout randomRow = guessLinearLayouts[row];
      String countryName = getCountryName(correctAnswer);
      ((Button) randomRow.getChildAt(column)).setText(countryName);    
   }

   private String getCountryName(String name) {
	   String country = name.substring(name.indexOf('-') + 1).replace('_', ' ');
	   country = country.substring(0, country.indexOf('&'));
      return country;
   } 
   private String getCapitalName(String name) {
	  return name.substring(name.indexOf('&') + 1, name.indexOf('*'));
   } 
   /*
   private void insertHighscores(int currentScore) {
	   SharedPreferences savedHighscores = this.getActivity().getSharedPreferences("pref", Context.MODE_PRIVATE);
	   SharedPreferences.Editor preferencesEditor = savedHighscores.edit();
	   
	   for (int i = 0; i < 5; i++) {
		   highscores[i] = savedHighscores.getInt("highscore" + String.valueOf(i), 0);
	   }
	   
	   highscores[5] = currentScore;
	   Arrays.sort(highscores);
	   
	   for (int i = 0; i < 5; i++) {
		   preferencesEditor.putInt("highscore" + String.valueOf(i), highscores[i]);
	   }
	   
	   preferencesEditor.apply(); 
	  
   }
   */
   private OnClickListener guessButtonListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         Button guessButton = ((Button) v); 
         String guess = guessButton.getText().toString();
         String answer = getCountryName(correctAnswer);
         ++totalGuesses;
         
         /*
         Resources res = getResources();
         String[] highScoreArray = res.getStringArray(R.array.high_scores);
         String score = "" + guessScore;
         highScoreArray.putString(score);
         */
         
         if (guess.equals(answer)) {
        	 if (firstTry) {
        		 firstTryCount++;
        		 guessScore += 2;
        	 } else {
        		 guessScore += 1;
        	 }
        	 
            ++correctAnswers;

            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));

            Context context = answerTextView.getContext();
            CharSequence text = "https://en.wikipedia.org/wiki/" + answer;
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            
            disableButtons();

            if (correctAnswers == FLAGS_IN_QUIZ) {
               DialogFragment quizResults = new DialogFragment() {
                     @Override
                     public Dialog onCreateDialog(Bundle bundle) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setCancelable(false);
                        if (player == 1 || player1Score == 0) {
                        	player1Score = guessScore;
                        	String player1Message = "Player 1's score is: " + player1Score;
                        	builder.setMessage(getResources().getString(R.string.results, totalGuesses, 
                        			(1000 / (double) totalGuesses), firstTryCount, player1Message));
                        	//insertHighscores(guessScore);

                        } else if (player == 2) {
                        	player2Score = guessScore;
                        	String player2Message = "Player 1's score is: " + player1Score + " \nPlayer 2's score is: " + player2Score;
                        	builder.setMessage(getResources().getString(R.string.results, totalGuesses, 
                        			(1000 / (double) totalGuesses), firstTryCount, player2Message));
                        	//insertHighscores(guessScore);
                        	newGame = true;
                        }
                        
                                                  
                        builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                        	public void onClick(DialogInterface dialog, int id) {
                                 resetQuiz();                                      
                              } 
                           }
                        );
                        
                        return builder.create();
                     }  
                  };
               
               quizResults.show(getFragmentManager(), "quiz results");
            } else {
            	
            	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                alertDialogBuilder.setTitle("Capital City");
                alertDialogBuilder.setMessage("Which is the capital city of " + answer + "?");
                alertDialogBuilder.setPositiveButton(getCapitalName(correctAnswer), new DialogInterface.OnClickListener() {
            		
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                       guessScore += 10;
           			
                    }
                 });
                alertDialogBuilder.setNeutralButton(otherOption1, null);
                alertDialogBuilder.setNegativeButton(otherOption2, null);
                AlertDialog dialog = alertDialogBuilder.create();
                dialog.show();
            	
               handler.postDelayed(new Runnable() { 
                     @Override
                     public void run() {
                        loadNextFlag();
                     }
                  }, 2000);
            } 
         } else {
        	firstTry = false;
            flagImageView.startAnimation(shakeAnimation);

            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); 
         } 
      } 
   };

   private void disableButtons() {
      for (int row = 0; row < guessRows; row++) {
         LinearLayout guessRow = guessLinearLayouts[row];
         for (int i = 0; i < guessRow.getChildCount(); i++)
            guessRow.getChildAt(i).setEnabled(false);
      } 
   } 
   
}
