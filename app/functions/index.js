// package com.vedantinfinity.babblingbubble

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

//import firebase functions modules
const functions = require('firebase-functions');
//import admin module
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);


// Listens for new messages added to messages/:pushId
exports.pushNotification = functions.database.ref('/messages/{pushId}').onWrite( event => {

    console.log('Push notification event triggered');

    //  Grab the current value of what was written to the Realtime Database.
    var valueObject = event.after.val();
    const messageTitle = valueObject.messageUser + " @ " + valueObject.messageBubble

    // Create a notification
    const payload = {
        notification: {
            title: messageTitle,
            body: valueObject.messageText, //|| valueObject.photoUrl,
        },
    };

    console.log("Payload for the notification: ", valueObject.messageUser, valueObject.messageText)

  //Create an options object that contains the time to live for the notification and the priority
    const options = {
        priority: "high",
        timeToLive: 60 * 60 * 0
    };

    return admin.messaging().sendToTopic(valueObject.messageBubble.replace(/\s/g,''), payload, options)
	   .then(function (response) {
               console.log("Successfully sent message: ", payload, options, response);
           }).catch(function (error) {
               console.log("Error sending message: ", error);
           });

});
