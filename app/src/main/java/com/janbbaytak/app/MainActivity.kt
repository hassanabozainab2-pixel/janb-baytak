package com.janbbaytak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import kotlinx.coroutines.delay

private val Green = Color(0xFF16734A)
private val Gold = Color(0xFFD7A83E)
private val Ink = Color(0xFF20352B)
private val Pale = Color(0xFFF4F7F3)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { JanbBaytakApp() } }
}
private data class Shop(val name:String,val type:String,val address:String,val phone:String,val products:List<String>)
private val shops = listOf(
 Shop("رضوان ماركت","سوبر ماركت","شارع البحر - إسنا","01147973350",listOf("مواد غذائية","منظفات")),
 Shop("البدوي","بقالة","شارع أحمد عرابي - إسنا","01101143675",listOf("بقالة","مشروبات")),
 Shop("أحمد محمود","كهربائي","داخل إسنا","01276873467",listOf("كهرباء منازل","صيانة")),
 Shop("محمود أحمد","حداد","شارع البحر - إسنا","01576890654",listOf("أعمال حدادة","أبواب"))
)
private val categories = listOf("🛒" to "سوبر ماركت","🍽️" to "مطاعم","👕" to "ملابس","🔧" to "صيانة","💄" to "تجميل","🏠" to "خدمات منزلية","📱" to "موبايلات","🚚" to "توصيل")

@Composable fun JanbBaytakApp() {
 var splash by remember { mutableStateOf(true) }; var page by remember { mutableStateOf("home") }; var query by remember { mutableStateOf("") }
 LaunchedEffect(Unit) { delay(2500); splash = false }
 MaterialTheme(colorScheme = lightColorScheme(primary=Green,secondary=Gold,background=Pale)) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
   if (splash) Splash() else Scaffold(containerColor=Pale,bottomBar={
    NavigationBar(containerColor=Color.White) {
     listOf("home" to "الرئيسية","search" to "بحث","add" to "أضف نشاطك","account" to "حسابي").forEach { (key,label) ->
      NavigationBarItem(selected=page==key,onClick={page=key},icon={Text(when(key){"home"->"⌂";"search"->"⌕";"add"->"+";else->"☻"},fontSize=21.sp)},label={Text(label,fontSize=10.sp)})
     }
    }
   }) { pad ->
    when(page) {
     "search" -> SearchPage(query,{query=it},Modifier.padding(pad))
     "add" -> AddPage(Modifier.padding(pad))
     "account" -> AccountPage(Modifier.padding(pad))
     else -> HomePage(query,{query=it},{page="search"},Modifier.padding(pad))
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
@Composable private fun HomePage(q:String,onQ:(String)->Unit,openSearch:()->Unit,modifier:Modifier=Modifier) {
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
   Column { Text("أهلاً بيك 👋",color=Ink); Text("جنب بيتك",color=Green,fontSize=27.sp,fontWeight=FontWeight.ExtraBold) }; Text("🏡",fontSize=34.sp)
  } }
  item { Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(18.dp)).padding(14.dp)) {
   Text("دور على منتج أو خدمة أو محل",color=Ink,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); SearchBox(q,onQ,openSearch)
  } }
  item { SectionTitle("الأقسام","اكتشف الخدمات القريبة") }
  item { LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   items(categories) { (emoji,label) -> Column(Modifier.width(112.dp).background(Color.White,RoundedCornerShape(16.dp)).clickable{openSearch()}.padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally) {
    Text(emoji,fontSize=27.sp); Text(label,color=Ink,fontSize=12.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
   } }
  } }
  item { SectionTitle("أنشطة قريبة منك","تواصل مع صاحب النشاط مباشرة") }
  items(shops) { ShopCard(it) }
  item { Column(Modifier.fillMaxWidth().background(Green,RoundedCornerShape(18.dp)).padding(18.dp)) {
   Text("عندك محل أو نشاط؟",color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold); Text("ضيف نشاطك وخلي عملاء أكتر يوصلولك",color=Color.White)
  } }
 }
}
@Composable private fun SearchPage(q:String,onQ:(String)->Unit,modifier:Modifier=Modifier) {
 val found=shops.filter { s -> q.isBlank() || s.name.contains(q,true) || s.type.contains(q,true) || s.address.contains(q,true) || s.products.any{it.contains(q,true)} }
 Column(modifier.fillMaxSize().padding(16.dp)) {
  Text("البحث الشامل",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink); Spacer(Modifier.height(12.dp)); SearchBox(q,onQ){}
  Spacer(Modifier.height(16.dp)); Text(if(q.isBlank()) "كل الأنشطة" else "نتائج البحث: ${found.size}",color=Ink,fontWeight=FontWeight.Bold)
  Spacer(Modifier.height(8.dp))
  if(found.isEmpty()) Text("مفيش نتائج مطابقة. جرّب كلمة تانية.",Modifier.fillMaxWidth().padding(30.dp),textAlign=TextAlign.Center,color=Color.Gray)
  else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(found){ShopCard(it)}}
 }
}
@Composable private fun AddPage(modifier:Modifier=Modifier) {
 var name by remember{mutableStateOf("")}; var category by remember{mutableStateOf("")}; var phone by remember{mutableStateOf("")}; var address by remember{mutableStateOf("")}
 Column(modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("أضف نشاطك",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink); Text("عرّف الناس بنشاطك وخدماتك",color=Color.Gray)
  Field("اسم النشاط",name){name=it}; Field("القسم",category){category=it}; Field("رقم الهاتف",phone){phone=it}; Field("العنوان",address){address=it}
  Button(onClick={},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Green)){Text("متابعة إنشاء الحساب")}
  Text("حفظ البيانات سيتفعّل بعد ربط قاعدة البيانات.",fontSize=12.sp,color=Color.Gray)
 }
}
@Composable private fun AccountPage(modifier:Modifier=Modifier) {
 var phone by remember{mutableStateOf("")}; var password by remember{mutableStateOf("")}
 Column(modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("حسابي",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Ink); Text("التصفح متاح بدون تسجيل. الحساب اختياري للعملاء وأصحاب الأنشطة.",color=Color.Gray)
  Field("رقم الهاتف",phone){phone=it}; Field("الرقم السري",password){password=it}
  Button(onClick={},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Green)){Text("دخول")}
  OutlinedButton(onClick={},modifier=Modifier.fillMaxWidth()){Text("إنشاء حساب جديد")}
  Text("المصادقة الآمنة ستُفعّل بعد ربط قاعدة البيانات.",fontSize=12.sp,color=Color.Gray)
 }
}
@Composable private fun SearchBox(value:String,onValue:(String)->Unit,onSubmit:()->Unit) {
 Row(Modifier.fillMaxWidth().background(Pale,RoundedCornerShape(12.dp)).padding(horizontal=12.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically) {
  Text("⌕",fontSize=22.sp,color=Green); Spacer(Modifier.width(8.dp))
  BasicTextField(value,onValue,Modifier.weight(1f),singleLine=true,decorationBox={inner->if(value.isEmpty())Text("ابحث عن محل أو منتج أو خدمة...",color=Color.Gray,fontSize=13.sp);inner()})
  TextButton(onClick=onSubmit){Text("بحث",color=Green)}
 }
}
@Composable private fun Field(label:String,value:String,onValue:(String)->Unit) {
 Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
  Text(label,color=Ink,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
  BasicTextField(value,onValue,Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(12.dp)).padding(14.dp),singleLine=true,decorationBox={inner->if(value.isEmpty())Text(label,color=Color.LightGray);inner()})
 }
}
@Composable private fun SectionTitle(title:String,subtitle:String) {
 Column { Text(title,fontSize=19.sp,color=Ink,fontWeight=FontWeight.Bold); Text(subtitle,fontSize=12.sp,color=Color.Gray) }
}
@Composable private fun ShopCard(shop:Shop) {
 Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(16.dp)).padding(14.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   Box(Modifier.size(48.dp).background(Pale,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Text("🏪",fontSize=25.sp)}
   Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
    Text(shop.name,color=Ink,fontWeight=FontWeight.Bold,fontSize=16.sp); Text(shop.type,color=Green,fontSize=12.sp); Text("📍 ${shop.address}",color=Color.Gray,fontSize=11.sp)
   }
  }
  Spacer(Modifier.height(8.dp)); Text("منتجات وخدمات: ${shop.products.joinToString(" • ")}",color=Color.DarkGray,fontSize=12.sp)
  Text("☎ ${shop.phone}",color=Green,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
 }
}
