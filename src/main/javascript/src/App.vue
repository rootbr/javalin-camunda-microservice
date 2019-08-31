<template>
  <div id="app">
    <notifications group="events"
                   position="bottom right"
                   animation-type="velocity"
                   :speed="500" />
    <div id="nav">
      <router-link to="/">view</router-link> |
      <router-link to="/edit">edit</router-link>
    </div>
    <router-view/>
  </div>
</template>
