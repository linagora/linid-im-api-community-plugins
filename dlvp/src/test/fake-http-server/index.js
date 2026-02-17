const express = require('express');
const app = express();
const port = 3001;

app.use(express.json());

app.get('/v1/test_api/types', (_httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({
            content: [
                { name: 'TYPE1' },
                { name: 'TYPE2' },
                { name: 'TYPE3' }
            ],
            page: 0,
            size: 10,
            totalElements: 3
        });
});

app.get('/v1/test_api/types/with-headers', (httpRequest, httpResponse) => {
    const authHeader = httpRequest.headers['authorization'];

    if (authHeader !== 'Bearer my-token') {
        httpResponse.status(401)
            .send({
                error: 'Unauthorized'
            });
        return;
    }

    httpResponse.status(200)
        .send({
            content: [
                { name: 'SECURE_TYPE1' },
                { name: 'SECURE_TYPE2' }
            ],
            page: 0,
            size: 10,
            totalElements: 2
        });
});

app.get('/v1/test_api/types/empty', (_httpRequest, httpResponse) => {
    httpResponse.status(200)
        .send({
            content: [],
            page: 0,
            size: 10,
            totalElements: 0
        });
});

app.get('/v1/test_api/400', (_httpRequest, httpResponse) => {
    httpResponse.status(400)
        .send({
            error: 'true',
        });
});

app.get('/v1/test_api/404', (_httpRequest, httpResponse) => {
    httpResponse.status(404)
        .send({
            error: 'true',
        });
});

app.get('/v1/test_api/500', (_httpRequest, httpResponse) => {
    httpResponse.status(500)
        .send({
            error: 'true',
        });
});

app.get('/v1/test_api/invalid-json', (_httpRequest, httpResponse) => {
    httpResponse.status(200)
        .type('text/plain')
        .send('this is not valid json');
});

app.post('/v1/test_api/types/search', (httpRequest, httpResponse) => {
    const filter = httpRequest.body.filter || '';

    const allTypes = [
        { name: 'TYPE1' },
        { name: 'TYPE2' },
        { name: 'TYPE3' }
    ];

    const filtered = filter
        ? allTypes.filter(t => t.name.includes(filter))
        : allTypes;

    httpResponse.status(200)
        .send({
            content: filtered,
            page: 0,
            size: 10,
            totalElements: filtered.length
        });
});

app.listen(port, () => {
    console.log(`Fake http server running on http://localhost:${port}`);
});
