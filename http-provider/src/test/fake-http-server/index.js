const express = require('express');
const app = express();
const port = 3000;

app.use(express.json());

app.get('/v1/test_api/400', (httpRequest, httpResponse) => {
    httpResponse.status(400)
        .send({
            error: "true",
        });
});

app.get('/v1/test_api/500', (httpRequest, httpResponse) => {
    httpResponse.status(500)
        .send({
            error: "true",
        });
});

app.post('/v1/test_api/user', (httpRequest, httpResponse) => {
    httpResponse.status(201)
        .send({
            id: 1,
            firstname: "John",
            lastname: "Doe",
            email: "john.doe@gmail.com",
            test: httpRequest.body.test,
            status: "created"
        });
});

app.get('/v1/test_api/user', (httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({
            pagination: {
                page: 0,
                size: 1,
                total: 1
            },
            content: [{
                id: 1,
                firstname: "John",
                lastname: "Doe",
                email: "john.doe@gmail.com",
                status: "retrieved"
            }]
        });
});

app.get('/v1/test_api/user/1', (httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({
            id: 1,
            firstname: "John",
            lastname: "Doe",
            email: "john.doe@gmail.com",
            status: "retrieved"
        });
});

app.delete('/v1/test_api/user/1', (httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({status: "deleted"});
});

app.delete('/v1/test_api/user/2', (httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({status: "not_deleted"});
});

app.put('/v1/test_api/user/1', (httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({
            id: 1,
            firstname: "John",
            lastname: "Doe",
            email: "john.put@gmail.com",
            status: "updated"
        });
});

app.patch('/v1/test_api/user/1', (httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({
            id: 1,
            firstname: "John",
            lastname: "Doe",
            email: "john.patch@gmail.com",
            status: "updated"
        });
});

app.listen(port, () => {
    console.log(`🟢 Fake http server running on http://localhost:${port}`);
});
