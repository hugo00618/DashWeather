console.log('This would be the main JS file.');

$(document).ready(function() {
    setTimeout(function() {
        $("#logoContainer").addClass("show");
        setTimeout(function() {
            $(".fade-in").addClass("show");
        }, 300);
    }, 500);
});