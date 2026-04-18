package com.example.recipeapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.recipeapp.Adapters.IngredientsAdapter;
import com.example.recipeapp.Adapters.InstructionsAdapter;
import com.example.recipeapp.Adapters.SimilarRecipeAdapter;
import com.example.recipeapp.Listeners.InstructionsListener;
import com.example.recipeapp.Listeners.RecipeClickListener;
import com.example.recipeapp.Listeners.RecipeDetailsListener;
import com.example.recipeapp.Listeners.SimilarRecipesListener;
import com.example.recipeapp.Models.InstructionsResponse;
import com.example.recipeapp.Models.RecipeDetailsResponse;
import com.example.recipeapp.Models.SimilarRecipeResponse;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailsActivity extends AppCompatActivity {
    int id;
    TextView textView_meal_name, textView_meal_source,textView_meal_summary;
    ImageView imageView_meal_image;
    RecyclerView recycler_meal_ingredients, recycler_meal_similar,recycler_meal_instructions;
    RequestManager manager;
    ProgressDialog dialog;
    IngredientsAdapter ingredientsAdapter;
    SimilarRecipeAdapter similarRecipeAdapter;
    InstructionsAdapter instructionsAdapter;
    SpeechRecognizer speechRecognizer;
    Intent speechIntent;
    int currentStep = 0;
    int totalSteps = 0;
    ImageView btn_mic;
    ScrollView scrollView;
    TextView text_similar;
    boolean isListening = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);
        findViews();
        btn_mic = findViewById(R.id.btn_mic);
        scrollView = findViewById(R.id.scrollView);
        text_similar = findViewById(R.id.text_similar);
        id = Integer.parseInt(getIntent().getStringExtra("id"));
        manager = new RequestManager(this);
        manager.getRecipeDetails(recipeDetailsListener,id);
        manager.getSimilarRecipes(similarRecipesListener, id);
        manager.getInstructions(instructionsListener,id);
        // xin quyền mic
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
        }
        // init speech
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        // listener
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                // LỖI HAY GẶP NHẤT LÀ ĐÂY: Nếu bạn không nói gì, nó sẽ văng lỗi ERROR_NO_MATCH hoặc ERROR_SPEECH_TIMEOUT
                // Giải pháp: Nếu isListening vẫn là true, cứ bật lại ép nó nghe tiếp!
                if (isListening) {
                    speechRecognizer.startListening(speechIntent);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);

                if (data != null && data.size() > 0) {
                    String text = data.get(0).toLowerCase();
                    Toast.makeText(RecipeDetailsActivity.this,
                            "You said: " + text,
                            Toast.LENGTH_SHORT).show();
                    handleVoiceCommand(text);
                }

                // Xử lý lệnh xong thì bật nghe lại (Nếu người dùng chưa bấm nút Tắt)
                if (isListening) {
                    speechRecognizer.startListening(speechIntent);
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
        btn_mic.setOnClickListener(v -> {
            if (isListening) {
                // Đang bật -> Ấn vào để TẮT
                isListening = false;
                speechRecognizer.stopListening();
                Toast.makeText(this, "Mic turned OFF", Toast.LENGTH_SHORT).show();
            } else {
                // Đang tắt -> Ấn vào để BẬT
                isListening = true;
                speechRecognizer.startListening(speechIntent);
                Toast.makeText(this, "Mic turned ON. please said next step,step back,step number,similar", Toast.LENGTH_SHORT).show();
            }
        });
        dialog = new ProgressDialog(this);
        dialog.setTitle("Loading Details...");
        dialog.show();
    }
    private void findViews() {
        textView_meal_name = findViewById(R.id.textView_meal_name);
        textView_meal_source = findViewById(R.id.textView_meal_source);
        textView_meal_summary = findViewById(R.id.textView_meal_summary);
        imageView_meal_image = findViewById(R.id.imageView_meal_image);
        recycler_meal_ingredients = findViewById(R.id.recycler_meal_ingredients);
        recycler_meal_similar = findViewById(R.id.recycler_meal_similar);
        recycler_meal_instructions = findViewById(R.id.recycler_meal_instructions);
    }

    private final RecipeDetailsListener recipeDetailsListener = new RecipeDetailsListener() {
        @Override
        public void didFetch(RecipeDetailsResponse response, String message) {
            dialog.dismiss();
            textView_meal_name.setText(response.title);
            textView_meal_source.setText(response.sourceName);
            textView_meal_summary.setText(response.summary);
            Picasso.get().load(response.image).into(imageView_meal_image);
            recycler_meal_ingredients.setHasFixedSize(true);
            recycler_meal_ingredients.setLayoutManager(new LinearLayoutManager(RecipeDetailsActivity.this, LinearLayoutManager.HORIZONTAL,false));
            ingredientsAdapter = new IngredientsAdapter(RecipeDetailsActivity.this, response.extendedIngredients);
            recycler_meal_ingredients.setAdapter(ingredientsAdapter);
        }

        @Override
        public void didError(String message) {
            Toast.makeText(RecipeDetailsActivity.this,message,Toast.LENGTH_SHORT).show();
        }
    };
    private final SimilarRecipesListener similarRecipesListener = new SimilarRecipesListener() {
        @Override
        public void didFetch(List<SimilarRecipeResponse> response, String message) {
            Log.d("SIZE", "Similar size: " + response.size());
            recycler_meal_instructions.setNestedScrollingEnabled(false);
            recycler_meal_similar.setHasFixedSize(true);
            recycler_meal_similar.setLayoutManager(new LinearLayoutManager(RecipeDetailsActivity.this,LinearLayoutManager.HORIZONTAL,false));
            similarRecipeAdapter = new SimilarRecipeAdapter(RecipeDetailsActivity.this,response,recipeClickListener);
            recycler_meal_similar.setAdapter(similarRecipeAdapter);
        }

        @Override
        public void didError(String message) {
            Toast.makeText(RecipeDetailsActivity.this,message,Toast.LENGTH_SHORT).show();
        }
    };

    private final RecipeClickListener recipeClickListener = new RecipeClickListener() {
        @Override
        public void onRecipeClicked(String id) {
            startActivity(new Intent(RecipeDetailsActivity.this,RecipeDetailsActivity.class)
                    .putExtra("id",id));
        }
    };

    private final InstructionsListener instructionsListener = new InstructionsListener() {
        @Override
        public void didFetch(List<InstructionsResponse> response, String message) {
            recycler_meal_instructions.setHasFixedSize(true);
            recycler_meal_instructions.setLayoutManager(new LinearLayoutManager(RecipeDetailsActivity.this,LinearLayoutManager.VERTICAL,false));
            instructionsAdapter = new InstructionsAdapter(RecipeDetailsActivity.this,response);
            recycler_meal_instructions.setAdapter(instructionsAdapter);
            recycler_meal_instructions.setNestedScrollingEnabled(false);
            if (response != null && response.size() > 0) {
                totalSteps = response.get(0).steps.size();
                Log.d("TOTAL_STEP", "Total: " + totalSteps);
            }
        }

        @Override
        public void didError(String message) {
            Toast.makeText(RecipeDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

    private void scrollToStep(int stepIndex) {
        // stepIndex đang là 0, 1, 2... tương ứng với Step 1, Step 2, Step 3...
        // Ta cần tìm TextView có nội dung là "1", "2", "3"...
        String targetStepText = String.valueOf(stepIndex + 1);

        scrollView.post(() -> {
            // Tìm View chứa con số của bước đó
            View targetView = findStepView(recycler_meal_instructions, targetStepText);

            if (targetView != null) {
                // Tính tọa độ Y tuyệt đối của View đó so với đỉnh của ScrollView
                int scrollToY = getRelativeTop(targetView, scrollView);

                // Trừ đi một chút (ví dụ 50px) để không bị dính sát mép màn hình
                scrollView.smoothScrollTo(0, scrollToY - 50);
            } else {
                Toast.makeText(this, "Không tìm thấy bước " + targetStepText, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm đệ quy: Quét toàn bộ View để tìm chính xác TextView chứa số của Step
    private View findStepView(ViewGroup parent, String stepNumber) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                // Kiểm tra ID và nội dung text xem có khớp với số Step cần tìm không
                if (tv.getId() == R.id.textView_instructions_step_number && tv.getText().toString().equals(stepNumber)) {
                    // Trả về view cha (LinearLayout/CardView) để lấy tọa độ mép trên cùng của thẻ
                    return (View) tv.getParent().getParent();
                }
            } else if (child instanceof ViewGroup) {
                // Nếu là ViewGroup (LinearLayout, RecyclerView...), tiếp tục quét vào trong
                View found = findStepView((ViewGroup) child, stepNumber);
                if (found != null) return found;
            }
        }
        return null;
    }

    // Hàm tính toán khoảng cách Y từ một View con đến ScrollView cha
    private int getRelativeTop(View myView, View parentView) {
        if (myView.getParent() == parentView || myView.getParent() == null) {
            return myView.getTop();
        } else {
            return myView.getTop() + getRelativeTop((View) myView.getParent(), parentView);
        }
    }

    private int extractNumber(String text) {
        if (text.contains("one")) return 1;
        if (text.contains("two")) return 2;
        if (text.contains("three")) return 3;
        if (text.contains("four")) return 4;
        if (text.contains("five")) return 5;
        if (text.contains("six")) return 6;
        if (text.contains("seven")) return 7;
        if (text.contains("eight")) return 8;
        if (text.contains("nine")) return 9;
        if (text.contains("ten")) return 10;
        if (text.contains("eleven")) return 11;
        if (text.contains("twelve")) return 12;
        if (text.contains("thirteen")) return 13;
        if (text.contains("fourteen")) return 14;
        if (text.contains("fifteen")) return 15;
        if (text.contains("sixteen")) return 16;
        String[] words = text.split(" ");
        for (String word : words) {
            try {
                return Integer.parseInt(word);
            } catch (Exception e) {}
        }
        return -1;
    }

    private void handleVoiceCommand(String text) {
        Log.d("VOICE_COMMAND", "Command: " + text);
        text = text.replace("type i", "eight")
                .replace(",", "")
                .replace("that's it", "six")
                .replace("for", "four")
                .replace("thickness", "next")
                .replace("tree", "three")
                .replace("type", "step")
                .replace("sit", "six")
                .replace("therefore", "four")
                .replace("thankful", "four")
                .replace("thefive", "five")
                .replace("thefall", "four")
                .replace("nest", "next");
        if (text.contains("next")) {
            Log.d("ACTION", "Next step");
            if (currentStep < totalSteps - 1) {
                currentStep++;
                scrollToStep(currentStep);
            }
        }

        else if (text.contains("back")) {
            if (currentStep > 0) {
                currentStep--;
                scrollToStep(currentStep);
            }
        }

        else if (text.contains("step")) {
            int number = extractNumber(text);
            Log.d("STEP", "Go to step: " + number);
            if (number >= 1 && number <= totalSteps) {
                currentStep = number - 1;
                scrollToStep(currentStep);
            }
        }

        else if (text.contains("similar")) {
            text_similar.post(() -> {
                scrollView.smoothScrollTo(0, text_similar.getTop());
            });
        }
    }
}
