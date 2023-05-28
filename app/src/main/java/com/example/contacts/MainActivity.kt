package com.example.contacts

import android.content.ContentResolver
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.contacts.ui.theme.SystemContactsExplorationTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.database.getStringOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SystemContactsExplorationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContactsList()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SystemContactsExplorationTheme {
        Greeting("Android")
    }
}

@Composable
fun ContactsList() {
    val contentResolver= LocalContext.current.contentResolver
    var contacts by remember {
      mutableStateOf(listOf<Contact>())
    }
    LaunchedEffect( true) {
        contacts= retrieveContacts(contentResolver)
    }
    LazyColumn{
      for (contact in contacts){
         item (contact.id){
             Text(text = contact.name)
         }
      }
    }
}
data class Contact(
    val id: String,
    val name: String,
)

fun retrieveContacts(contentResolver: ContentResolver): List<Contact> {
    val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null)
    val contacts = mutableListOf<Contact>()
    cursor?.use { cursor ->
        if (cursor.moveToFirst()) {
            do {
                val nameColumnIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val idColumnIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val name = cursor.getStringOrNull(nameColumnIndex).toString()
                val id = cursor.getString(idColumnIndex)
                contacts.add(Contact(id, name))
            } while (cursor.moveToNext())
        }
    }
    return contacts
}