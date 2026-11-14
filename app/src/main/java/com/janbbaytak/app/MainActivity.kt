package com.janbbaytak.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val SUPABASE_URL = "https://ijmeliyhdilfkveotadr.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_LGDX79mgcJIY58ioMTUf9Q_Lgea0b2z"
private val Green = Color(0xFF16734A)
private val Gold = Color(0xFFD7A83E)
private val Ink = Color(0xFF20352B)
private val Pale = Color(0xFFF4F7F3)

data class Business(val id:String,val name:String,val description:String,val address:String,val city:String,val phone:String,val whatsapp:String)
data class Product(val id:String,val name:String,val description:String,val price:String,val businessName:String,val phone:String)
data class UserSession(val userId:String,val accessToken:String)

private object Api {
    data class Result(val status:Int,val body:String) {
        fun json():JSONObject = try { JSONObject(body) } catch (_:Exception) { JSONObject() }
    }
    fun call(path:String, method:String="GET", body:JSONObject?=null, token:String?=null, prefer:String?=null):Result {
        val connection = (URL(SUPABASE_URL + path).openConnection() as HttpURLConnection)
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("apikey", SUPABASE_KEY)
        connection.setRequestProperty("Authorization", "Bearer ${token ?: SUPABASE_KEY}")
        connection.setRequestProperty("Content-Type", "application/json")
        if (prefer != null) connection.setRequestProperty("Prefer", prefer)
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()
        return Result(status,response)
    }
    fun businesses():List<Business> {
        val r=call("/rest/v1/businesses?select=id,name,description,address,city,phone,whatsapp&status=eq.approved&is_active=eq.true&order=is_featured.desc,created_at.desc")
        if (r.status !in 200..299) return emptyList()
        val a=JSONArray(r.body)
        return (0 until a.length()).map { i -> a.getJSONObject(i).let { Business(it.optString("id"),it.optString("name"),it.optString("description"),it.optString("address"),it.optString("city"),it.optString("phone"),it.optString("whatsapp")) } }
    }
    fun products():List<Product> {
        val r=call("/rest/v1/products?select=id,name,description,price,businesses(name,phone)&is_active=eq.true&order=created_at.desc")
        if (r.status !in 200..299) return emptyList()
        val a=JSONArray(r.body)
        return (0 until a.length()).map { i ->
            val o=a.getJSONObject(i); val b=o.optJSONObject("businesses") ?: JSONObject()
            Product(o.optString("id"),o.optString("name"),o.optString("description"),o.optString("price"),b.optString("name"),b.optString("phone"))
        }
    }
    fun signUp(phone:String,password:String):Result {
        val body=JSONObject().put("phone",phone).put("password",password)
        return call("/auth/v1/signup","POST",body)
    }
    fun signIn(phone:String,password:String):Result {
        val body=JSONObject().put("phone",phone).put("password",password)
        return call("/auth/v1/token?grant_type=password","POST",body)
    }
    fun addBusiness(session:UserSession,name:String,address:String,phone:String,description:String):Result {
        val body=JSONObject()
            .put("owner_id",session.userId)
            .put("name",name)
            .put("slug","janb-${UUID.randomUUID()}")
            .put("address",address)
            .put("phone",phone)
            .put("description",description)
        return call("/rest/v1/businesses","POST",body,session.accessToken,"return=representation")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JanbBaytakApp() }
    }
}

@Composable fun JanbBaytakApp() {
    var splash by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf("home") }
    var session by remember { mutableStateOf<UserSession?>(null) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1800); splash=false }
    MaterialTheme(colorScheme=lightColorScheme(primary=Green,secondary=Gold,background=Pale)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (splash) Splash() else Scaffold(
                containerColor=Pale,
                bottomBar={
                    NavigationBar(containerColor=Color.White) {
                        listOf("home" to "الرئيسية","search" to "بحث","add" to "أضف نشاطك","account" to "حسابي").forEach { (key,label) ->
                            NavigationBarItem(selected=page==key,onClick={page=key},icon={Text(when(key){"home"->"⌂";"search"->"⌕";"add"->"+";else->"☻"},fontSize=21.sp)},label={Text(label,fontSize=10.sp)})
                        }
                    }
                }
            ) { pad ->
                when(page) {
                    "search" -> SearchPage(Modifier.padding(pad))
                    "add" -> AddPage(session,Modifier.padding(pad),{page="account"})
                    "account" -> AccountPage(session,{session=it},Modifier.padding(pad))
                    else -> HomePage(Modifier.padding(pad),{page="search"})
                }
            }
        }
    }
}

@Composable private fun Splash() {
    Box(Modifier.fillMaxSize().background(Green),contentAlignment=Alignment.Center) {
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.size(112.dp).background(Color.White,RoundedCornerShape(30.dp)),contentAlignment=Alignment.Center){Text("🏡",fontSize=58.sp)}
            Spacer(Modifier.height(18.dp)); Text("جنب بيتك",color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)
            Text("كل اللي محتاجه قريب منك",color=Color(0xFFE6F2EA),fontSize=15.sp)
        }
    }
}

@Composable private fun HomePage(modifier:Modifier=Modifier,openSearch:()->Unit) {
    var businesses by remember { mutableStateOf(emptyList<Business>()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { businesses=withContext(Dispatchers.IO){Api.businesses()}; loading=false }
    LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Column { Text("أهلاً بيك 👋",color=Ink); Text("جنب بيتك",color=Green,fontSize=27.sp,fontWeight=FontWeight.ExtraBold) }
                Text("🏡",fontSize=34.sp)
            }
        }
        item {
            Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(18.dp)).padding(14.dp)) {
                Text("دور على محل أو منتج أو خدمة",color=Ink,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Button(onClick=openSearch,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Green)){Text("ابدأ البحث")}
            }
        }
        item { Text("الأنشطة المعتمدة",fontSize=19.sp,color=Ink,fontWeight=FontWeight.Bold) }
        if (loading) item { CircularProgressIndicator(color=Green) }
        else if (businesses.isEmpty()) item { EmptyState("لسه مفيش أنشطة معتمدة. تقدر تكون أول صاحب نشاط يضيف بياناته.") }
        else items(businesses) { BusinessCard(it) }
    }
}

@Composable private fun SearchPage(modifier:Modifier=Modifier) {
    var query by remember { mutableStateOf("") }
    var businesses by remember { mutableStateOf(emptyList<Business>()) }
    var products by remember { mutableStateOf(emptyList<Product>()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val data=withContext(Dispatchers.IO){ Pair(Api.businesses(),Api.products()) }
        businesses=data.first; products=data.second; loading=false
    }
    val q=query.trim()
    val foundBusinesses=businesses.filter { q.isBlank() || listOf(it.name,it.description,it.address,it.city).any { v->v.contains(q,true) } }
    val foundProducts=products.filter { q.isBlank() || listOf(it.name,it.description,it.businessName).any { v->v.contains(q,true) } }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("البحث الشامل",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("اسم منتج أو محل أو خدمة")})
        Spacer(Modifier.height(12.dp))
        if(loading) CircularProgressIndicator(color=Green)
        else if(foundBusinesses.isEmpty() && foundProducts.isEmpty()) EmptyState("مفيش نتائج مطابقة. جرّب كلمة تانية.")
        else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(foundProducts.isNotEmpty()) item { Text("المنتجات",fontWeight=FontWeight.Bold,color=Green,fontSize=18.sp) }
            items(foundProducts) { ProductCard(it) }
            if(foundBusinesses.isNotEmpty()) item { Text("الأنشطة",fontWeight=FontWeight.Bold,color=Green,fontSize=18.sp) }
            items(foundBusinesses) { BusinessCard(it) }
        }
    }
}

@Composable private fun AddPage(session:UserSession?,modifier:Modifier=Modifier,onNeedLogin:()->Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("أضف نشاطك",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink)
        Text("بيانات النشاط تُرسل للمراجعة قبل ظهورها للزوار.",color=Color.Gray)
        Field("اسم النشاط",name){name=it}
        Field("العنوان",address){address=it}
        Field("رقم الهاتف",phone){phone=it}
        Field("وصف النشاط",description){description=it}
        Button(onClick={
            if(session==null){message="سجّل دخولك الأول علشان تضيف نشاط.";onNeedLogin()}
            else if(name.isBlank() || phone.isBlank()){message="اكتب اسم النشاط ورقم الهاتف."}
            else {
                busy=true; message=""
                Thread {
                    val result=try{Api.addBusiness(session,name,address,phone,description)}catch(e:Exception){Api.Result(0,e.message ?: "تعذر الاتصال")}
                    Handler(Looper.getMainLooper()).post {
                        busy=false
                        message=if(result.status in 200..299) "تم إرسال النشاط للمراجعة بنجاح." else "تعذر الحفظ: ${result.json().optString("message",result.body)}"
                    }
                }.start()
            }
        },modifier=Modifier.fillMaxWidth(),enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Green)) {
            if(busy) CircularProgressIndicator(modifier=Modifier.size(18.dp),color=Color.White) else Text("إرسال النشاط")
        }
        if(message.isNotBlank()) Text(message,color=Green)
    }
}

@Composable private fun AccountPage(session:UserSession?,onSession:(UserSession?)->Unit,modifier:Modifier=Modifier) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("حسابي",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink)
        if(session!=null) {
            Text("تم تسجيل الدخول بنجاح.",color=Green,fontWeight=FontWeight.Bold)
            Text("يمكنك الآن إضافة نشاطك وإدارته.")
            OutlinedButton(onClick={onSession(null)},modifier=Modifier.fillMaxWidth()){Text("تسجيل الخروج")}
        } else {
            Text("التصفح متاح بدون تسجيل. الحساب اختياري للعملاء وأصحاب الأنشطة.",color=Color.Gray)
            Field("رقم الهاتف بصيغة دولية مثل +201xxxxxxxxx",phone){phone=it}
            Field("الرقم السري",password){password=it}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onClick={creating=false}){Text("دخول")}
                TextButton(onClick={creating=true}){Text("إنشاء حساب")}
            }
            Button(onClick={
                if(phone.isBlank() || password.length<8) message="اكتب رقم الهاتف والرقم السري (8 أحرف على الأقل)."
                else {
                    busy=true; message=""
                    Thread {
                        val result=try { if(creating) Api.signUp(phone,password) else Api.signIn(phone,password) } catch(e:Exception){Api.Result(0,e.message ?: "تعذر الاتصال")}
                        Handler(Looper.getMainLooper()).post {
                            busy=false
                            val json=result.json()
                            val token=json.optString("access_token")
                            val user=json.optJSONObject("user")
                            if(result.status in 200..299 && token.isNotBlank() && user!=null) {
                                onSession(UserSession(user.optString("id"),token)); message="تم تسجيل الدخول."
                            } else if(result.status in 200..299 && creating) {
                                message="تم إرسال طلب إنشاء الحساب. لو تأكيد الهاتف مفعّل، يلزم رمز SMS."
                            } else {
                                message=json.optString("msg",json.optString("message",result.body.ifBlank{"حصل خطأ في الاتصال."}))
                            }
                        }
                    }.start()
                }
            },modifier=Modifier.fillMaxWidth(),enabled=!busy,colors=ButtonDefaults.buttonColors(containerColor=Green)) {
                if(busy) CircularProgressIndicator(modifier=Modifier.size(18.dp),color=Color.White) else Text(if(creating)"إنشاء الحساب" else "دخول")
            }
            if(message.isNotBlank()) Text(message,color=Green,fontSize=13.sp)
            Text("ملحوظة: تفعيل تأكيد الهاتف برسالة SMS يحتاج مزود رسائل خارجي. لم نفعّل أي خدمة مدفوعة.",fontSize=11.sp,color=Color.Gray)
        }
    }
}

@Composable private fun Field(label:String,value:String,onValue:(String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(label,color=Ink,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
        BasicTextField(value,onValue,Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(12.dp)).padding(14.dp),singleLine=true,decorationBox={inner->if(value.isEmpty())Text(label,color=Color.LightGray,fontSize=12.sp);inner()})
    }
}
@Composable private fun BusinessCard(b:Business) {
    Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text(b.name,color=Ink,fontWeight=FontWeight.Bold,fontSize=17.sp)
        if(b.description.isNotBlank()) Text(b.description,color=Color.DarkGray,fontSize=12.sp)
        val address=listOf(b.address,b.city).filter{it.isNotBlank()}.joinToString(" - ")
        if(address.isNotBlank()) Text("📍 $address",color=Color.Gray,fontSize=12.sp)
        if(b.phone.isNotBlank()) Text("☎ ${b.phone}",color=Green,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
    }
}
@Composable private fun ProductCard(p:Product) {
    Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(p.name,color=Ink,fontWeight=FontWeight.Bold,fontSize=16.sp)
        if(p.description.isNotBlank()) Text(p.description,color=Color.DarkGray,fontSize=12.sp)
        if(p.price.isNotBlank() && p.price!="null") Text("${p.price} جنيه",color=Green,fontWeight=FontWeight.Bold)
        if(p.businessName.isNotBlank()) Text("المحل: ${p.businessName}",color=Color.Gray,fontSize=12.sp)
    }
}
@Composable private fun EmptyState(message:String) {
    Text(message,Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(14.dp)).padding(20.dp),color=Color.Gray,textAlign=TextAlign.Center)
}
