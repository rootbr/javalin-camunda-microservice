<template>
  <div id="app">
    <notifications group="events" />
    <div id="nav">
      <router-link to="/">view</router-link> |
      <router-link to="/edit">edit</router-link>
    </div>
    <router-view/>
  </div>
</template>
