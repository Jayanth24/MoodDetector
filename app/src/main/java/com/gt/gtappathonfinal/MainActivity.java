package com.gt.gtappathonfinal;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.util.ArrayList;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.media.MediaPlayer;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private final String apiEndpoint = "https://westus.api.cognitive.microsoft.com/face/v1.0";
    private final String subscriptionKey = "a93c47d4ae80443d82838201a25cc1d7";
    private final FaceServiceClient faceServiceClient = new FaceServiceRestClient(apiEndpoint, subscriptionKey);
    public final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    private ArrayList<Emotion> emotions;
    private double globalAge;
    private String globalGender;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button1 = findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            }
        });
        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySuggestedEmotionalActions();
            }
        });
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                displaySuggestedAgeActions();
            }
        });
        Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                displaySuggestedGenderActions();
            }
        });


          detectionProgressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       if(requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);
                detectAndFrame(bitmap);
            }

            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        AsyncTask<InputStream, String, Face[]> detectTask = new AsyncTask<InputStream, String, Face[]>() {
            String exceptionMessage = "";

            @SuppressLint("WrongThread")
            @Override
            protected Face[] doInBackground(InputStream... params) {
                try {
                    publishProgress("Detecting .... ");
                    final Face[] result = faceServiceClient.detect(
                            params[0],
                            true,
                            false,
                            new FaceServiceClient.FaceAttributeType[] {
                                   FaceServiceClient.FaceAttributeType.Age,
                                    FaceServiceClient.FaceAttributeType.Emotion,
                                    FaceServiceClient.FaceAttributeType.Gender
                            }
                    );

                    if(result == null) {
                        publishProgress("Detection Finished. Nothing detected");
                        return null;
                    }
                    publishProgress(String.format("Detection finished" + result.length + "faces detected."));


                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            TextView information = findViewById(R.id.textarea1);
                            double age = result[result.length - 1].faceAttributes.age;
                            globalAge = age;
                            String gender = result[result.length -1].faceAttributes.gender;
                            globalGender = gender;
                            information.setText("You are an individual with an approximate age of "+age+" and a gender of "+gender+"!");
                            Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
                            information.setTypeface(boldTypeface);
                            information.animate().scaleXBy(500).setDuration(1000000000).start();
                            information.animate().scaleYBy(500).setDuration(1000000000).start();
                            final MediaPlayer song = MediaPlayer.create(MainActivity.this,R.raw.song);
                            song.start();


                            // information.animate().alpha(0.0f).translationY(information.getHeight()).setDuration(10000);
                        }
                    });



                    return result;
                }catch(Exception e) {
                    exceptionMessage = String.format("Detection failed: %s", e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPreExecute() {
                detectionProgressDialog.show();
            }

            @Override
            protected void onProgressUpdate(String... progress) {
                detectionProgressDialog.setMessage(progress[0]);
            }

            @Override
            protected void onPostExecute(Face[] result) {
                detectionProgressDialog.dismiss();

                if(!exceptionMessage.equals("")) {
                    showError(exceptionMessage);
                }

                if(result == null) {
                    return;
                }

                ImageView imageView = findViewById(R.id.imageView1);
                imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result));
                imageBitmap.recycle();
            }

        };

        detectTask.execute(inputStream);

    }

    private void showError(String message) {
        new AlertDialog.Builder(this).setTitle("Error").setMessage(message).setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }}).create().show();
    }

    private Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
        //Note that the method also shows the emotions attributed to each person
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        ListView listView = findViewById(R.id.listview1);
        emotions = new ArrayList<>();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        ArrayList<String> emotionStrings = new ArrayList<String>();
        if(faces != null) {
            for(int i = 0; i < faces.length; i++) {
                FaceRectangle faceRectangle = faces[i].faceRectangle;
                canvas.drawRect(faceRectangle.left, faceRectangle.top,faceRectangle.left + faceRectangle.width, faceRectangle.top + faceRectangle.height, paint);
                Emotion emotion = faces[i].faceAttributes.emotion;
                emotions.add(emotion);
                double e1 = emotion.surprise;
                double e2 = emotion.anger;
                double e3 = emotion.contempt;
                double e4 = emotion.disgust;
                double e5 = emotion.fear;
                double e6 = emotion.happiness;
                double e7 = emotion.neutral;
                double e8 = emotion.sadness;
                String s1 = "Person "+i+": Surprise: "+e1;
                String s2 = "Person "+i+": Anger: "+e2;
                String s3 = "Person "+i+": Contempt: "+e3;
                String s4 = "Person "+i+": Disgust: "+e4;
                String s5 = "Person "+i+": Fear: "+e5;
                String s6 = "Person "+i+": Happiness: "+e6;
                String s7 = "Person "+i+": Neutral: "+e7;
                String s8 = "Person "+i+": Sadness: "+e8;
                emotionStrings.add(s1);
                emotionStrings.add(s2);
                emotionStrings.add(s3);
                emotionStrings.add(s4);
                emotionStrings.add(s5);
                emotionStrings.add(s6);
                emotionStrings.add(s7);
                emotionStrings.add(s8);
            }
        }



        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, emotionStrings);

        listView.setAdapter(adapter);

       return bitmap;
    }

    private void displaySuggestedAgeActions() {

        ArrayList<String> responses = new ArrayList<String>();
        ListView listview3 = findViewById(R.id.listview3);

        if(globalAge <= 10.0) {
            responses.add("You are between 0-10 years of age! Suggested activities:");
            responses.add("Play hide and seek with a blanket!");
            responses.add("Take a long and refreshing nap!");
            responses.add("Do your primary school/elementary school homework!");
            responses.add("Run around outside and soak up all the nature!");
            responses.add("Have fun and enjoy your youth while it lasts!");
        }
        else if(globalAge > 10.0 & globalAge <= 20.0) {
            responses.add("You are between 10-20 years of age! Suggested activities:");
            responses.add("Make sure to focus on your academics as a stepping stone for the future!");
            responses.add("Take care of yourself and ensure that you maintain proper sleep, exercise, and food!");
            responses.add("Start planning for college and the important future that lies ahead!");
            responses.add("Form close bonds with your friends and family that will last throughout your life!");
            responses.add("Learn to live life to the fullest without any shortcuts!");
        }
        else if(globalAge > 20.0 & globalAge <= 30.0) {
            responses.add("You are between 20-30 years of age! Suggested activities:");
            responses.add("Start looking for jobs and specifically plan for the future.");
            responses.add("Complete college with good grades and involve yourself in many personal projects.");
            responses.add("Consider continuing your education with a Masters Degree, PhD, or both degrees!");
            responses.add("Make connections in the industry and get practical experience through internships!");
            responses.add("Specialize completely in your field of interest and become very well versed in your craft.");
        }
        else if(globalAge > 30.0 & globalAge <= 40.0) {
            responses.add("You are between 30-40 years of age! Suggested activities:");
            responses.add("Get a good stable career path that interests you and will make you happy!");
            responses.add("Potentially consider marriage and settling down with a loved one.");
            responses.add("Start looking for ways to invest your money and maximize the profits that you make.");
            responses.add("Buy a house or an apartment in an area of the world that you truly love.");
            responses.add("Settle down in life and focus on the priorities that matter the most to you.");
        }
        else if(globalAge > 40.0 & globalAge <= 50.0) {
            responses.add("You are between 40-50 years of age! Suggested activities:");
            responses.add("Continue working hard in your job and accomplish all of your goals succesfully.");
            responses.add("Consider having children with your loved ones and discuss the matter thoroughly.");
            responses.add("Make sure to continue your good exercise and eating habits that you have developed in your youth.");
            responses.add("Reach out in your community and develop a sense of social connections and belonging with others.");
            responses.add("Try new things and explore your creative side! This might be your only chance to do it!");
        }
        else if(globalAge > 50.0 & globalAge <= 60.0) {
            responses.add("You are between 50-60 years of age! Suggested activities:");
            responses.add("Think about retirement options and when you will be wanting to call it a day.");
            responses.add("Establish a set of leisure activities that you can always fall back on and do at any time.");
            responses.add("Visit your alma mater (either high school or college) and connect with the current generation.");
            responses.add("Celebrate each day as if it your last and cherish every possible moment.");
            responses.add("Connect with your immediate and extended family and share with them the lessons you have learned along the way.");
        }
        else if(globalAge >= 60.0) {
            responses.add("You are greater than 60 years of age! Suggested activities:");
            responses.add("Congratulations on living this long! Share your deepest secrets to all of us!");
            responses.add("Make sure to continue doing basic mobility exercises like stretching or walking to guarantee your movement abilities.");
            responses.add("Get adequate amounts of exposure to sunlight or Vitamin D everyday, and make sure to go outside for at least an hour a day.");
            responses.add("Find something relaxing to do in your free time, and pick up a new hobby to do.");
            responses.add("Have fun for the rest of live, and pat yourself on the back for making it this long.");
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, responses);

        listview3.setAdapter(adapter);


    }

    private void displaySuggestedGenderActions() {

        ArrayList<String> responses = new ArrayList<>();
        ListView listview4 = findViewById(R.id.listview4);

        if(globalGender.equals("male")) {
          responses.add("You are a male! Suggested activities:");
          responses.add("Start cooking and get on the grill outdoors!");
          responses.add("If it's a winter month currently, try skiing and snowboarding!");
          responses.add("Activate your mental side by investing in the stock market!");
          responses.add("Try picking up a new sport to play, like archery or chess!");
          responses.add("Get a manly tattoo! After all, you only live once!");

        }
        else if(globalGender.equals("female")) {
            responses.add("You are a female! Suggested activities:");
            responses.add("Try knitting or sewing as a way to espouse the feminine personality within you.");
            responses.add("Take care of yourself by going to a personal grooming session at a salon!");
            responses.add("Explore your artistic personality by drawing or painting a real subject!");
            responses.add("Tune into nature by either riding a horse or walking your dog!");
            responses.add("Relax and chill out by watching a movie with your friends and family!");
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, responses);

        listview4.setAdapter(adapter);

    }

    private void displaySuggestedEmotionalActions() {
        ArrayList<String> responses = new ArrayList<String>();
        ListView listView2 =  findViewById(R.id.listview2);
        for(Emotion e: emotions) {
            ArrayList<Double> ranges = new ArrayList<>();
            ranges.add(e.surprise);
            ranges.add(e.anger);
            ranges.add(e.contempt);
            ranges.add(e.disgust);
            ranges.add(e.fear);
            ranges.add(e.happiness);
            ranges.add(e.neutral);
            ranges.add(e.sadness);

            double max = 0;
            int index = 0;

            for(int i = 0; i< ranges.size(); i++) {
                if(ranges.get(i) > max) {
                    max = ranges.get(i);
                    index = i;
                }
            }

            switch(index) {
                case 0:
                    responses.add("Primary emotion: Surprise! Suggestions:");
                    responses.add("Take a deep breath and relax.");
                    responses.add("Figure out who or what is surprising you.");
                    responses.add("Contemplate why you are so happy and espouse the positivity.");
                    responses.add("Enjoy the surprise and cherish the moment.");

                 break;
                case 1:
                    responses.add("Primary emotion: Anger! Suggestions:");
                    responses.add("Stop immediately and employ the ten second relaxation technique.");
                    responses.add("Take very deep breaths and relax.");
                    responses.add("Understand the underlying stimulus of the anger and take methods to rectify it accordingly.");
                    responses.add("Vow to never be angry again.");
                    break;
                case 2:
                    responses.add("Primary emotion: Contempt! Suggestions:");
                    responses.add("Learn to show respect for other people no mattter who they are.");
                    responses.add("Try to identify some positive traits of everyone each day.");
                    responses.add("Show love and receive love - underlying mantra!");
                    responses.add("Obtain a new outlook and approach on life moving forward");
                    break;
                case 3:
                    responses.add("Primary emotion: Disgust! Suggestions:");
                    responses.add("Immediately try to become calm by doing one of your hobbies.");
                    responses.add("Take a long period of time to step away from your work and tend to yourself");
                    responses.add("Prioritize self care over seeming academic success.");
                    responses.add("Obtain realization of how emotions can completely change your personality");
                    break;
                case 4:
                    responses.add("Primary emotion: Fear! Suggestions:");
                    responses.add("Try to do something that is known to always calm you down.");
                    responses.add("Understand the phobia or disorder that is causing your fear.");
                    responses.add("See a psychiatrist or psychologist to try to understand the root cause of the issue.");
                    responses.add("Attach yourself to something tangible that is bound to build up possible memories.");
                    break;
                case 5:
                    responses.add("Primary emotion: Happiness! Suggestions:");
                    responses.add("Truly cherish the moment and jot it down for future usage.");
                    responses.add("Spend more time with your friends and family to further continue this line of thinking.");
                    responses.add("Have a goal in your life to strive to be happy for at lest half of the days of the week.");
                    responses.add("Use this positivity as a way to increase your self-esteem and self-efficacy.");

                    break;
                case 6:
                    responses.add("Primary emotion: Neutral! Suggestions:");
                    responses.add("Try to do something that makes you feel a certain emotion, preferably happiness!");
                    responses.add("Become excited in your life and take pride in the things that you do!");
                    responses.add("Find something meaningful that you can hold on to and use it moving forward.");
                    responses.add("Learn to recognize that it is OK to display emotion in life and that you should be willing to.");
                    break;
                case 7:
                    responses.add("Primary emotion: Sadness! Suggestions:");
                    responses.add("Stop being sad and replace the negative thoughts with positives through cognitive restructuring.");
                    responses.add("Learn to forget the bad and espouse the good, and use this principle as a mantra that defines you.");
                    responses.add("Have close friends and family that you can confide in and discuss these underlying probems.");
                    responses.add("Mark a recurring emotional factor that causes you to feel disappointed and eliminate it from your life.");
                    break;
                default:
                    responses.add("No Dominant Emotion!");
                    responses.add("Lighten up!");
                    break;

            }

            ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, responses);

            listView2.setAdapter(adapter);

        }

    }
}
