package com.client.alex.e_kapsimi;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.client.Model.MyResponse;
import com.client.Model.Notification;
import com.client.Model.Order;
import com.client.Model.Request;
import com.client.Model.Sender;
import com.client.Model.Token;
import com.client.alex.e_kapsimi.Common.Common;
import com.client.alex.e_kapsimi.Database.Database;
import com.client.alex.e_kapsimi.Remote.APIService;
import com.client.alex.e_kapsimi.ViewHolder.CartAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.hoang8f.widget.FButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Cart extends AppCompatActivity {

    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    FirebaseDatabase mDatabase;
    DatabaseReference requests;

    TextView txtTotalPrice;
    FButton btnTotalPlace;

    List<Order> cart =new ArrayList<>();
    CartAdapter adapter;

    APIService mAPIService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        //Firebase
        mDatabase=FirebaseDatabase.getInstance();
        requests= mDatabase.getReference("Requests");

        //Init Service
        mAPIService=Common.getFCMService();


        //Init
        mRecyclerView=(RecyclerView)findViewById(R.id.listCart);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager= new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        txtTotalPrice=(TextView)findViewById(R.id.total);
        btnTotalPlace=(FButton)findViewById(R.id.btnPlaceOrder);

        btnTotalPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(cart.size()>0)
                {
                    showAlertDialog();
                }
                else
                {
                    Toast.makeText(Cart.this, "Το καλάθι είναι άδειο!!!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        loadListFood();
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialog =new AlertDialog.Builder(Cart.this);
        alertDialog.setTitle("Σχόλια παραγγελίας!");
        alertDialog.setMessage("Αφήστε ένα σχόλιο π.χ πως θέλετε να είναι ο καφές σας.");

        final EditText edtsurname=new EditText(Cart.this);
        LinearLayout.LayoutParams lp= new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
                );

        edtsurname.setLayoutParams(lp);

        alertDialog.setView(edtsurname); //add edit text to alert dialog
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("Επιβεβαίωση", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Create new request
                Request request=new Request(
                        Common.current_user.getPhone(),
                        Common.current_user.getName(),
                        edtsurname.getText().toString(),
                        txtTotalPrice.getText().toString(),
                        cart
                );

                //Submit to firebase
                //We will use System.CurrentMilli to key
                String order_number=String.valueOf(System.currentTimeMillis());
                requests.child(order_number).setValue(request);

                //Delete cart
                new Database(getBaseContext()).cleanCart();

                sendNotificationOrder(order_number);


                Toast.makeText(Cart.this,"Η παραγγελία ολοκληρώθηκε!!",Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        alertDialog.setNegativeButton("Ακύρωση", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });

        alertDialog.show();
    }

    private void sendNotificationOrder(final String order_number) {
        DatabaseReference tokens=FirebaseDatabase.getInstance().getReference("Tokens");
        Query data=tokens.orderByChild("isServerToken").equalTo(true);
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot postSnapShot:dataSnapshot.getChildren())
                {
                    Token serverToken= postSnapShot.getValue(Token.class);
                    //Create raw payload to send
                    Notification notification=new Notification("Νέα παραγγελία!!","e-ΚΨΜ "+order_number);
                    Sender content=new Sender(serverToken.getToken(),notification);

                    mAPIService.sendNotification(content)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if(response.code()==200) {
                                        if (response.body().success == 1) {
                                            Toast.makeText(Cart.this, "Η παραγγελία ολοκληρώθηκε!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(Cart.this, "Αποτυχία!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                    Log.e("ERROR",t.getMessage());
                                }
                            });

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void loadListFood() {
        cart = new Database(this).getCarts();
        adapter=new CartAdapter(cart,this);
        adapter.notifyDataSetChanged();
        mRecyclerView.setAdapter(adapter);

        //Calculate Total price

        double total=0;
        for(Order order:cart)
        {
            total+=(Double.parseDouble(order.getPrice()))*(Double.parseDouble(order.getQuantity()));
        }
        Locale locale= new Locale("en","GR");
        NumberFormat fmt=NumberFormat.getCurrencyInstance(locale);

        txtTotalPrice.setText(fmt.format(total));


    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if(item.getTitle().equals(Common.DELETE))
        {
            deleteCart(item.getOrder());
        }
        return true;
    }

    private void deleteCart(int position) {
        //We will remove item at List<Order> by position
        cart.remove(position);
        //After that we will delete all sql data from sqlite
        new Database(this).cleanCart();
        //Finally we will update data from List<Order> to sqlite
        for (Order item:cart)
        {
            new Database(this).addToCart(item);
        }
        //refresh
        loadListFood();

    }
}
