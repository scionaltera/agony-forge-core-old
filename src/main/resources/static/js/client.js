var socket = null;
var stompClient = null;
var isReconnecting = false;
var reconnectDelay = 2;
var commandHistory = [];
var commandHistoryIndex = -1;
var commandHistoryLength = 50;
var scrollBackLength = 500;

$(document).ready(function () {
    $("form").submit(function (event) {
        sendInput();
        event.preventDefault();
        return false;
    });

    connect();
});

$(document).keydown(function (event) {
    if ("" === $("form input:not(.d-none)").val()) {
        return true;
    }

    if (event.which === 9) { // tab
        if (event.preventDefault()) {
            event.preventDefault();
            return false;
        }
    }
});

$(document).keyup(function (event) {
    if (event.which === 38) { // up arrow
        commandHistoryIndex++;

        if (commandHistoryIndex >= commandHistory.length) {
            commandHistoryIndex = commandHistory.length - 1;
        }

        if (commandHistoryIndex >= 0) {
            $("form input:not(.d-none)").val(commandHistory[commandHistoryIndex]);
        }
    } else if (event.which === 40) { // down arrow
        commandHistoryIndex--;

        if (commandHistoryIndex < 0) {
            commandHistoryIndex = -1;
        }

        if (commandHistoryIndex >= 0) {
            $("form input:not(.d-none)").val(commandHistory[commandHistoryIndex]);
        } else {
            $("form input:not(.d-none)").val("");
        }
    }
});

function connect() {
    socket = new SockJS('/mud');
    stompClient = webstomp.over(socket, { heartbeat : false, protocols: ['v12.stomp'] });
    stompClient.connect(
        {},
        function (frame) {
            console.log('Connected: ' + frame);
            showOutput(["[green]Connected to server."]);

            reconnectDelay = 2;

            stompClient.subscribe('/user/queue/output', function (message) {
                    var msg = JSON.parse(message.body);
                    var plainInput = $("#user-input");
                    var passwordInput = $("#user-password");

                    if (msg.secret) {
                        plainInput.addClass("d-none");
                        passwordInput.removeClass("d-none");
                        passwordInput.focus();
                    } else {
                        passwordInput.addClass("d-none");
                        plainInput.removeClass("d-none");
                        plainInput.focus();
                    }

                    showOutput(msg.output);
                },
                {});
        },
        function () {
            var actualDelay = Math.random() * reconnectDelay;

            if (isReconnecting === false) {
                showOutput(["[red]Disconnected from server. Will attempt to reconnect in " + actualDelay.toFixed(0) + " seconds."]);

                isReconnecting = true;

                setTimeout(function () {
                    console.log('Disconnected.');
                    showOutput(["[dyellow]Reconnecting to server..."]);

                    isReconnecting = false;

                    this.connect();
                }, actualDelay * 1000);

                reconnectDelay = Math.min(reconnectDelay * 2, 128);
            }
        });
}

function sendInput() {
    var inputBox = $("form input:not(.d-none)");

    if (inputBox[0].type === 'text') {
        commandHistoryIndex = -1;
        commandHistory.unshift(inputBox.val());

        if (commandHistory.length > commandHistoryLength) {
            commandHistory.pop();
        }

        $("#output-list").find("li:last-child").append("<span class='yellow'> " + htmlEscape(inputBox.val()) + "</span>");
    } else {
        $("#output-list").find("li:last-child").append("<span class='yellow'> ********</span>");
    }

    stompClient.send("/app/input", JSON.stringify({'input': inputBox.val()}));
    inputBox.val('');
}

function showOutput(message) {
    var outputBox = $("#output-box");
    var outputList = $("#output-list");

    for (var i = 0; i < message.length; i++) {
        if ("" === message[i]) {
            outputList.append("<li>&nbsp;</li>");
        } else {
            outputList.append("<li>" + replaceColors(message[i]) + "</li>");
        }
    }

    outputBox.prop("scrollTop", outputBox.prop("scrollHeight"));

    var scrollBackOverflow = outputList.find("li").length - scrollBackLength;

    if (scrollBackOverflow > 0) {
        outputList.find("li").slice(0, scrollBackOverflow).remove();
    }
}

function replaceColors(message) {
    return String(message).replace(/\[(\w+)]/g, "<span class='$1'>");
}

function htmlEscape(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\//g, '&#x2F;');
}
