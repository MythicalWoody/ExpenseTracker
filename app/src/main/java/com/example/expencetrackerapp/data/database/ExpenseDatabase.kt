package com.example.expencetrackerapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expencetrackerapp.data.database.dao.CategoryDao
import com.example.expencetrackerapp.data.database.dao.ExpenseDao
import com.example.expencetrackerapp.data.database.dao.MerchantDao
import com.example.expencetrackerapp.data.database.entities.Category
import com.example.expencetrackerapp.data.database.entities.Expense
import com.example.expencetrackerapp.data.database.entities.MerchantMapping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
        entities = [Expense::class, Category::class, MerchantMapping::class],
        version = 1,
        exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantDao(): MerchantDao

    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                ExpenseDatabase::class.java,
                                                "expense_database"
                                        )
                                        .addCallback(DatabaseCallback())
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    prepopulateCategories(database.categoryDao())
                }
            }
        }

        private suspend fun prepopulateCategories(categoryDao: CategoryDao) {
            val defaultCategories =
                    listOf(
                            Category(
                                    name = "Food & Dining",
                                    icon = "Restaurant",
                                    color = "#FF6B6B",
                                    keywords =
                                            "swiggy,zomato,dominos,mcdonalds,kfc,burger king,pizza hut,starbucks,cafe,restaurant,food,dining,eat,meal,lunch,dinner,breakfast,hotel,bistro,kitchen,bar,pub,brewery,bakery,cake,sweet,chai,coffee,tea,dhaba,bhojanalaya,mess,canteen,eats,foods,haldiram,subway,taco bell"
                            ),
                            Category(
                                    name = "Shopping",
                                    icon = "ShoppingCart",
                                    color = "#4ECDC4",
                                    keywords =
                                            "amazon,flipkart,myntra,ajio,meesho,nykaa,tatacliq,reliance,dmart,bigbasket,blinkit,zepto,instamart,grocery,mart,store,shopping,zudio,decathlon,uniqlo,croma,vijay sales,lenskart,titan,bata,mamaearth,sugar,urbanic,shoppers stop,lifestyle,max,pantaloons,westside,trends,fabindia,clothes,fashion,mall"
                            ),
                            Category(
                                    name = "Transport",
                                    icon = "DirectionsCar",
                                    color = "#45B7D1",
                                    keywords =
                                            "uber,ola,rapido,metro,petrol,diesel,fuel,parking,toll,irctc,railway,redbus,bus,cab,taxi,auto,blusmart,namma yatri,fastag,hpcl,bpcl,ioc,shell,yulu,vogo,bounce"
                            ),
                            Category(
                                    name = "Fashion & Lifestyle",
                                    icon = "Checkroom",
                                    color = "#96CEB4",
                                    keywords =
                                            "zara,h&m,levis,nike,adidas,puma,reebok,asics,under armour,sketchers,crocs,salon,spa,haircut,grooming,cosmetics,jewellery,tanishq,malabar,joyalukkas,caratlane"
                            ),
                            Category(
                                    name = "Entertainment",
                                    icon = "Movie",
                                    color = "#DDA0DD",
                                    keywords =
                                            "netflix,prime video,hotstar,spotify,gaana,youtube,bookmyshow,pvr,inox,cinepolis,movie,theatre,concert,game,gaming,sonyliv,zee5,jiocinema,aha,steam,playstation,xbox,club,party,event"
                            ),
                            Category(
                                    name = "Bills & Utilities",
                                    icon = "Receipt",
                                    color = "#F7DC6F",
                                    keywords =
                                            "electricity,water,gas,internet,broadband,wifi,mobile,recharge,airtel,jio,vi,vodafone,bsnl,bill,utility,maintenance,bescom,bwssb,mahanagar,adani gas,tata power,act fibernet,hathway,spectranet,dth,tatasky,dish tv,sun direct"
                            ),
                            Category(
                                    name = "Health",
                                    icon = "LocalHospital",
                                    color = "#82E0AA",
                                    keywords =
                                            "hospital,clinic,doctor,pharmacy,medicine,1mg,pharmeasy,netmeds,apollo,medplus,healthkart,gym,fitness,yoga,cult.fit,practo,medibuddy,dr lal,thyrocare,lab,diagnostic,scan,mri,xray"
                            ),
                            Category(
                                    name = "Education",
                                    icon = "School",
                                    color = "#85C1E9",
                                    keywords =
                                            "school,college,university,course,udemy,coursera,skillshare,book,stationery,tuition,coaching,exam,test,kindle,audible,class,fees,library,learning"
                            ),
                            Category(
                                    name = "Travel",
                                    icon = "Flight",
                                    color = "#F8B500",
                                    keywords =
                                            "makemytrip,goibibo,cleartrip,yatra,oyo,airbnb,booking.com,trivago,flight,airline,indigo,spicejet,vistara,airindia,akasa,ixigo,easemytrip,agoda,hotel,resort,vacation,trip,tour,visa,forex,irctc"
                            ),
                            Category(
                                    name = "Investments",
                                    icon = "TrendingUp",
                                    color = "#27AE60",
                                    keywords =
                                            "zerodha,groww,upstox,angelone,paytm money,mutual fund,sip,stocks,investment,trading,kite,coin,smallcase,indmoney,ppf,nps,lic,insurance,premium,policy"
                            ),
                            Category(
                                    name = "Transfers",
                                    icon = "SwapHoriz",
                                    color = "#9B59B6",
                                    keywords =
                                            "transfer,upi,neft,imps,rtgs,sent to,paid to,loan,emi,repayment,credit card,rent"
                            ),
                            Category(
                                    name = "Others",
                                    icon = "Category",
                                    color = "#95A5A6",
                                    keywords = "misc,general,unknown"
                            )
                    )

            categoryDao.insertCategories(defaultCategories)
        }
    }
}
