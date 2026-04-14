package com.example.recipeapp.Adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

public class SimilarRecipeAdapter {
}
class SimilarRecipeViewHolder extends RecyclerView.ViewHolder {
    CardView similar_recipe_holder;
    TextView textView_similar_title, textView_similar_serving;
    ImageView imageView_similar;
    public SimilarRecipeViewHolder(@NonNull View itemView) {
        super(itemView);
    }
}