define(function() {
    return {
        read: function(str) {
            return str.split('\n');
        },
        write: function(obj) {
            // If an array, extract the self URI and join using newline
            if (obj instanceof Array) {
                return obj.map(resource => {
                    return resource._links.self.href;
                }).join('\n');
            } else {
                return obj._links.self.href;
            }
        }
    }
});
