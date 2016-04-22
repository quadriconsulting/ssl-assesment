<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head lang="en">
    <title>CSV Upload</title>
    <link rel="stylesheet" href="css/bootstrap.min.css"/>
</head>
<body>
    <%
        String userName = null;
        String sessionID = null;
        Cookie[] cookies = request.getCookies();
        if(cookies !=null){
            for(Cookie cookie : cookies){
                if(cookie.getName().equals("user")) userName = cookie.getValue();
            }
        }
    %>
    <form action="LogoutServlet" method="post">
        <h4>Hi <%=userName %>!</h4><button type="submit" class="btn btn-default navbar-btn">Logout</button>
    </form>

    <div class="container">
        <div class="row">
            <div class="col-xs-offset-3 col-xs-6">
                <form name="fileSendForm" method="POST" action="uploadCSV" enctype="multipart/form-data">
                    <div class="form-group">
                        <label for="fileCSV">File input</label>
                        <input type="file" name="fileCSV" id="fileCSV" accept="text/csv"/>
                    </div>
                    <button type="submit" class="btn btn-default">Upload</button>
                </form>
            </div>
        </div>
    </div>

</body>
</html>
