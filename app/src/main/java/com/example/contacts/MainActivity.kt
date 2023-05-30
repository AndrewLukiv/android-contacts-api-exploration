package com.example.contacts

import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.contacts.ui.theme.SystemContactsExplorationTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.database.getStringOrNull
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SystemContactsExplorationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "contacts") {
                        composable("contacts") {
                            ContactsList(onNavigateToRawContacts = { contactId ->
                                navController.navigate("$contactId/raw_contacts")
                            })
                        }
                        composable("{contact_id}/raw_contacts") { backStackEntry ->
                            RawContactsList(contactId = backStackEntry.arguments?.getString("contact_id")!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RawContactsList(contactId: String) {
    val contentResolver = LocalContext.current.contentResolver
    var rawContacts by remember {
        mutableStateOf(listOf<RawContact>())
    }
    LaunchedEffect(true) {
        retrieveRawContacts(contactId, contentResolver)?.also { rawContacts = it }
    }
    LazyColumn {
        for (rawContact in rawContacts) {
            item(rawContact.id) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = rawContact.accountType, style = MaterialTheme.typography.titleLarge)
                    Text(text = rawContact.accountName, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun ContactsList(onNavigateToRawContacts: (String) -> Unit) {
    val contentResolver = LocalContext.current.contentResolver
    var contacts by remember {
        mutableStateOf(listOf<Contact>())
    }
    LaunchedEffect(true) {
        retrieveContacts(contentResolver)?.also { contacts = it }
    }
    LazyColumn {
        for (contact in contacts) {
            item(contact.id) {
                Text(text = contact.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable { onNavigateToRawContacts(contact.id) })
            }
        }
    }
}

data class Contact(
    val id: String,
    val name: String,
)

fun retrieveContacts(contentResolver: ContentResolver): List<Contact>? {
    val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null)
    val contacts = cursor?.toIterable {
        val nameColumnIndex =
            it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        val idColumnIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
        Contact(
            id = it.getString(idColumnIndex), name = it.getStringOrNull(nameColumnIndex).toString()
        )
    }?.toList()
    cursor?.close()
    return contacts
}

data class RawContact(
    val id: Long, val accountName: String, val accountType: String,
)

fun retrieveRawContacts(
    contactLookupId: String, contentResolver: ContentResolver
): List<RawContact>? {
    val contactCursor = contentResolver.query(
        ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(contactLookupId)
            .build(), arrayOf(ContactsContract.Contacts._ID), null, null
    )
    val contactId = contactCursor?.toIterable {
        val idColumnIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
        it.getLong(idColumnIndex)
    }?.firstOrNull()
    contactCursor?.close()
    return contactId?.let { id ->
        val cursor = contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            null,
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(id.toString()),
            null
        )
        val rawContacts = cursor?.toIterable {
            val accountTypeColumnIndex =
                it.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)
            val accountNameColumnIndex =
                it.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
            val idColumnIndex =
                it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)
            RawContact(
                id = it.getLong(idColumnIndex),
                accountName = it.getString(accountNameColumnIndex),
                accountType = it.getString(accountTypeColumnIndex)
            )
        }?.toList()

        cursor?.close()
        rawContacts
    }
}

class CursorIterator<T>(private val cursor: Cursor, private val converter: (Cursor) -> T) :
    Iterator<T> {
    private var isFirst = true
    override fun hasNext(): Boolean {
        return !cursor.isLast
    }

    override fun next(): T {
        if (isFirst) {
            cursor.moveToFirst()
            isFirst = false
        } else {
            cursor.moveToNext()
        }
        return converter(cursor)
    }

}

fun <T> Cursor.toIterable(converter: (Cursor) -> T): Iterable<T> {
    return object : Iterable<T> {
        override fun iterator(): Iterator<T> {
            return CursorIterator(this@toIterable, converter)
        }
    }
}