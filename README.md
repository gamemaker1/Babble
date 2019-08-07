# ![Babble][https://github.com/gamemaker1/Babble/Screenshots/chat.png]  Babble
Want an open source alternative to Whatsapp to stay in touch with small groups - your family, friends, office groups? This is an open source chat app built for Android using Firebase as the backend. You can change the code to point to your own instance of Firebase - that will make your data completely private to you. 

# Features
**Sign-in/Sign-up** - Using email-password login. You can signup with your own email and password. Users signed up are called Babblers.

**Groups (Called Bubbles)** - Can be created by entering your friends' email address(es). It will also allow you to automatically send an email to the friend, alerting him/her via email that you have added him/her to the Bubble.

**Chatting** - You can send plain text messages and images from your phone to any Bubble (chat group). The Babble app also allows you to invoke the share functionality from any app, browser included, on your phone and share text/links with Babblers on the Bubble. 

**Todos** - You can add todos to each Bubble (group). Anyone in the Bubble can claim and complete the todos. 

**Privacy** - This app uses [Firebase](https://firebase.google.com/) to manage authentication and store its data. Firebase is completely secure and trusted by a large number of tech companies. None of your data is sold to any other companies. Ideally you should host it on your own instance of Firebase to keep data extremely private to yourself. If you directly use the app from the Playstore, it will be using a common instance of Firebase (which is secure and your data safe). 


# Installation
Go to Google Play Store and search for 'Babble Open Source Chat App'.


# Known issues
These are known issues in the code. Contributors are welcome; please send me a pull request:
1. When you first install the app, the app will show the Home screen (`GroupsActivity`) with a `ProgressBar` going round and round forever. This is because you are not signed in and so, it cannot access the database. To get the app to work, you need to press the menu icon and then press the Sign Out option. This signs you out and gets you to the sign-in screen and then the app works smoothly.
2. When you add a friend, it deletes the entire info about the group, including the messages and todos, and then recreates the group with the same Bubble (Chat group) name and id and the list of people in the group. The reason behind this is that in the `GroupItem` class, there is a setter for setting an ArrayList as the list of the people in the Bubble. But that setter doesn't work, I have no idea why.
3. You cannot add multiple friends at once.
4. There are no settings for the user (notifications, change password, update app, view app info, etc.)

Apart from this, we still need to:
1. Create a web app using the same database and Auth providers and same interface.
2. Add support for [Solid](https://solid.inrupt.com/) (an initiative by Sir Tim Berners Lee) to store data. That way your data is private and portable. 
3. Add useful comments in the code to let others know what we want to do.

# License Info
Copyright (C) 2019  Vedant K

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.


**Thank you for contributing to or spreading word about this app!**
