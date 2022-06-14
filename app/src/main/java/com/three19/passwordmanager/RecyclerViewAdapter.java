package com.three19.passwordmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    public ArrayList<Data> myList;
    public ArrayList<Data> myList_copy;

    private Context context;

    // TODO: Major Bugs are fixed except for these:
    //FIXME: 4) Display lottie animation on deleting an item, lottie icon is present in raw folder


    RecyclerViewAdapter(ArrayList<Data> myList, Context context){
        this.myList = myList;
        this.context = context;
        myList_copy = new ArrayList<>(myList);
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custome_layout_recyclerview,parent,false);
        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
        public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        holder.account.setText(myList.get(position).account_name);
        holder.user_name.setText(myList.get(position).user_name);
        holder.password.setText(myList.get(position).password);


        holder.pencil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(context, ChangePassword.class);
                in.putExtra("account", holder.account.getText());
                in.putExtra("username", holder.user_name.getText());
                in.putExtra("password", holder.password.getText());
                context.startActivity(in);

            }
        });


    }
    public Filter getFilter() {
        return filter;
    }

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Data> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(myList_copy);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Data item : myList_copy) {
                    if (item.getAccount_name().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }


        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            myList.clear();
            myList.addAll((ArrayList) results.values);
            notifyDataSetChanged();
        }
    };


        @Override
    public int getItemCount() {
        return myList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder  {

        CardView cardView;
        TextView account, password, user_name;
        ImageView pencil;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = (CardView) itemView.findViewById(R.id.cardView_id_recycler);
            account = (TextView) itemView.findViewById(R.id.main_account_id_text);
            user_name = (TextView) itemView.findViewById(R.id.acc_card_id);
            password = (TextView) itemView.findViewById(R.id.pass_card_id);

            pencil = (ImageView) itemView.findViewById(R.id.imageview_create_icon);





        }

    }
}
